package org.bumblebyte

import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import os.Path
import scalatags.Text
import scalatags.Text.all.*

import java.io.InputStreamReader
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Collections
import scala.util.chaining.*

@main
def generate(): Unit =
  val extensions = Collections.singletonList(YamlFrontMatterExtension.create())
  val parser     = Parser.builder().extensions(extensions).build()
  val renderer   = HtmlRenderer.builder().escapeHtml(false).build()

  val strings = os.walk(os.pwd / "posts", skip = _.ext != "md").map { path =>
    val inputStream = os.read.inputStream(path)
    val node        = parser.parseReader(new InputStreamReader(inputStream))

    val metadata = new YamlFrontMatterVisitor().tap(node.accept).pipe(_.getData)
    val html     = renderer.render(node)

    val publishedDate = metadata.get("date").getFirst

    val file = Templates.base(Seq(h1(path.baseName), raw(html)))
    os.write.over(os.pwd / s"$publishedDate.html", file.render)
    path.baseName -> s"$publishedDate.html"
  }

  val yoe = 1 + ChronoUnit.YEARS.between(LocalDate.of(2017, 9, 1), LocalDate.now())
  val index = Seq(
    p(s"Polyglot software engineer with $yoe years of experience."),
    ul(strings.map { (title, fileRef) =>
      li(a(href := fileRef)(title))
    }*)
  )

  val `index.html` =
    s"""
       |<!DOCTYPE html>
       |${Templates.base(index).render}
       |""".stripMargin

  os.write.over(os.pwd / "index.html", `index.html`)