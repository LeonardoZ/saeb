val ys = "2010" :: "2012" :: "201407" :: "2016" :: "201607" :: "201608" :: Nil
val zs = ys.map {
  case x if x.length == 4 => ys.filter(_.startsWith(x)).last
  case x if x.length == 6 =>
    if (ys.filter(_.startsWith(x.substring(0, 4))).size == 1) x else ""
}.filter(!_.isEmpty)

ys.filter(y => zs.contains(y))

