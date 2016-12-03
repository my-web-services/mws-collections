package papers

import macros.Model

@Model
case class Paper(title: String, author: String, domain: String, link: String)
