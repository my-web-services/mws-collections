package macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.collection.immutable.Seq
import scala.meta._

@compileTimeOnly("@Model not expanded")
class Model extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {

    type Fields = Seq[Term.Param]

    // Add account field
    def accountify(name: Type.Name, fields: Fields): Defn.Class =
      q"case class $name (..$fields, accountId: String)"

    // Define JSON serialization/deserialization
    def jsonify(name: Type.Name): Stat =
      q"implicit val jsonFormat: play.api.libs.json.OFormat[$name] = play.api.libs.json.Json.format[$name]"

    // Define Slick Table class
    def constructFields(fields: Fields, clazz: Ctor.Ref.Name): Seq[Stat] = {
      var init = true
      val fieldsDeclarations: Seq[Stat] = fields map { f =>
        val fieldName = Pat.Var.Term(Term.Name(f.name.value))
        val fieldType = Type.Name(f.decltpe.get.syntax)
        val fieldLit: Lit = Lit(f.name.value)

        if (init) {
          init = false
          q"val $fieldName: Rep[$fieldType] = column[$fieldType]($fieldLit, O.PrimaryKey)"
        }
        else
          q"val $fieldName: Rep[$fieldType] = column[$fieldType]($fieldLit)"
      }

      val fieldNames: Seq[Term] = fields map (f => Term.Name(f.name.value))

      val projection = q"def * = (..$fieldNames) <> ({$clazz.apply _}.tupled, $clazz.unapply)"
      fieldsDeclarations :+ projection
    }

    def slickify(name: String, fields: Fields): Seq[Stat] = {
      val table: Type.Name = Type.Name(s"${name}Table")
      val lowercase = name.toLowerCase
      q"""
         import _root_.play.api.db.slick.HasDatabaseConfigProvider
         import _root_.slick.driver.JdbcProfile

         trait $table { self: HasDatabaseConfigProvider[JdbcProfile] =>
           import driver.api._

           class $table(tag: Tag) extends Table[${Type.Name(name)}](tag, ${Lit(lowercase)}) {
             ..${constructFields(fields, Ctor.Ref.Name(name))}
           }

           lazy val ${Pat.Var.Term(Term.Name(s"${lowercase}s"))} = TableQuery[$table]
         }
       """.stats
    }

    def repify(name: String, fields: Fields): Seq[Stat] = {
      val table = Ctor.Ref.Name(s"${name}Table")
      val repo = Type.Name(s"${name}Repository")
      val entityType = Type.Name(name)
      val entity = Term.Name(name.toLowerCase)
      val entityList = Term.Name(s"${entity.value}List")
      val entities = Term.Name(s"${entity.value}s")
      val primary = fields.head
      val primName = Term.Name(fields.head.name.value)
      q"""
          import _root_.com.google.inject.{Inject, Singleton}
          import _root_.play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
          import _root_.slick.driver.JdbcProfile
          import _root_.scala.concurrent.Future

          @Singleton()
          class $repo @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
            extends $table with HasDatabaseConfigProvider[JdbcProfile] {

              import driver.api._

              def insert($entity: $entityType) =
                db.run { $entities += $entity }

              def insertAll($entityList: List[$entityType]) =
                db.run { $entities ++= $entityList }

              def update($entity: $entityType): Future[Int] =
                db.run { $entities.filter(_.$primName === $entity.$primName).update($entity) }

              def delete($primary): Future[Int] =
                db.run { $entities.filter(_.$primName === $primName).delete }

              def getAll: Future[List[$entityType]] =
                db.run { $entities.to[List].result }

              def getAllByUser(userName: String): Future[List[$entityType]] =
                db.run {$entities.filter(_.accountId === userName).to[List].result }

              def getById($primary): Future[Option[$entityType]] =
                db.run { $entities.filter(_.$primName === $primName).result.headOption }

              def ddl = $entities.schema
          }
       """.stats
    }

    defn match {
      case Defn.Class(_, name, _, Ctor.Primary(_, _, paramss), _) =>
        val main = name.value

        // Add accountId
        val clazz: Defn.Class = accountify(name, paramss.head)
        // Add JSON implicit conversion
        val json: Stat = jsonify(name)
        // Add slick classes
        val slick: Seq[Stat] = slickify(main, clazz.ctor.paramss.head)
        // Add repository
        val repo: Seq[Stat] = repify(main, clazz.ctor.paramss.head)

        // Companion object
        val obj: Defn.Object =
          q"""
            object ${Term.Name(name.value)} {
              ..${Seq(json) ++ slick ++ repo}
            }
           """

        // Add class mapping in state
        val fields = paramss.head
            .map { p => (p.name.value, p.decltpe.get.syntax) }
            .map { case (n, t) => s"$n:$t" } mkString "$"

        if (!Generators.hasBeenGenerated(main))
          Generators.writeToFile(s"./macros_project/macros/state/$main", fields)

        // Return class + companion
        Term.Block(Seq(clazz, obj))
      case _ =>
        abort("@Model must annotate a class.")
    }
  }
}

