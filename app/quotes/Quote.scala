package quotes

import macros.Model

@Model
case class Quote(quote: String, author: String, category: String)
