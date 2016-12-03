package talks

import macros.Model

@Model
case class Talk(title: String, speaker: String, link: String)
