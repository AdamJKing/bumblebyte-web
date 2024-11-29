package org.bumblebyte

import scalatags.Text.all.*
import scalatags.Text.tags2
import scalatags.stylesheet.Cls
import scalatags.stylesheet.StyleSheet

object Templates:

  private object Style extends StyleSheet:

    initStyleSheet()

    val base: Cls = cls()

  def base(pageName: String, contents: Seq[Frag]): Frag =
    html(
      lang := "en",
      head(
        link(rel  := "stylesheet", href   := "https://cdn.simplecss.org/simple.min.css"),
        meta(name := "copyright", content := "© 2024 BumbleByte Software Ltd"),
        meta(name := "license", content   := "https://creativecommons.org/licenses/by-nc-nd/4.0/"),
        tags2.title(pageName)
      ),
      body(
        Style.base,
        header(a(href := "index.html")(h1("BumbleByte Software")), h2("Independent Software Contracting")),
        tags2.main(contents*),
        footer(
          p("BumbleByte Software Ltd is a company registered in England and Wales. Registered number: 16009956. Registered office: 3rd Floor, 86-90 Paul Street, London, United Kingdom, EC2A 4NE."),
          p(
            "© 2024 BumbleByte Software Ltd. Blog content is licensed under a Creative Commons Attribution-NonCommercial-NoDerivs (CC BY-NC-ND) license. ",
            a(href := "https://creativecommons.org/licenses/by-nc-nd/4.0/", "Learn more.")
          )
        )
      )
    )
