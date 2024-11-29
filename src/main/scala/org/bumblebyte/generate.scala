package org.bumblebyte

import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import scalatags.Text
import scalatags.Text.all.*
import scalatags.Text.tags2

import java.io.InputStreamReader
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Collections
import scala.util.chaining.*

case class BlogPost(title: String, publishedDate: LocalDate, filename: String)

@main
def generate(): Unit =
  val extensions = Collections.singletonList(YamlFrontMatterExtension.create())
  val parser     = Parser.builder().extensions(extensions).build()
  val renderer   = HtmlRenderer.builder().escapeHtml(false).build()

  val strings = os
    .walk(os.pwd / "posts", skip = _.ext != "md")
    .map { path =>
      val inputStream = os.read.inputStream(path)
      val node        = parser.parseReader(new InputStreamReader(inputStream))

      val metadata = new YamlFrontMatterVisitor().tap(node.accept).pipe(_.getData)
      val html     = renderer.render(node)

      val publishedDate =
        LocalDate.parse(Option(metadata.get("date")).fold(sys.error(s"$path had no `publishedDate`"))(_.getFirst))

      val file = Templates.base(path.baseName, Seq(h1(path.baseName), raw(html)))
      os.write.over(os.pwd / s"$publishedDate.html", s"<!DOCTYPE html>${file.render}")
      BlogPost(path.baseName, publishedDate, s"$publishedDate.html")
    }
    .sortBy(_.publishedDate)(Ordering[LocalDate].reverse)

  val yoe = 1 + ChronoUnit.YEARS.between(LocalDate.of(2017, 9, 1), LocalDate.now())
  val index = Seq(
    p(s"Polyglot software engineer with $yoe years of experience."),
    tags2.nav(
      ul(strings.map { case BlogPost(title, publishedDate, fileRef) =>
        li(a(attr("aria-current") := "page", href := fileRef)(s"$publishedDate - $title"))
      }*)
    )
  )

  val `index.html` =
    s"""<!DOCTYPE html>
       |${Templates.base("Home", index).render}
       |""".stripMargin

  os.write.over(os.pwd / "index.html", `index.html`)
