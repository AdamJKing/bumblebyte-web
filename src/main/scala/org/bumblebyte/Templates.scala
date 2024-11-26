package org.bumblebyte

import scalatags.Text.all.*
import scalatags.Text.tags2

object Templates:

  private val LegalDisclaimer =
    """
      |BumbleByte Software Ltd is a company registered in England and Wales. Registered number: 16009956. Registered
      |office: 3rd Floor, 86-90 Paul Street, London, United Kingdom, EC2A 4NE.
      |""".stripMargin

  def base(pageName: String, contents: Seq[Frag]): Frag =
    html(
      lang := "en",
      head(
        meta(name := "copyright", content := "© 2024 Your Name"),
        meta(name := "license", content   := "https://creativecommons.org/licenses/by-nc-nd/4.0/"),
        tags2.title(pageName)
      ),
      body(a(href := "index.html")(h1("BumbleByte Software")), h2("Independent Software Contracting"))(contents)(
        footer(LegalDisclaimer)
      )
    )
