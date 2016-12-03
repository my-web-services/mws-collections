package books

import macros.Model

@Model
case class Book(title: String, author: String, year: Int)
