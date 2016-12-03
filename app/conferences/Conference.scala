package conferences

import macros.Model

@Model
case class Conference(title: String, period: String, link: String)
