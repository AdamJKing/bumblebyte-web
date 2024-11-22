package org.bumblebyte

import scalatags.Text.all.Frag

import java.time.LocalDate

case class BlogPost(title: String, date: LocalDate, contents: Frag)
