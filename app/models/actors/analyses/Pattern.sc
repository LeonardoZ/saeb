val xs = (1 to 10).toList
println(xs)

def runT(xs: List[Int]): Unit = xs match {
  case last :: Nil => println(last * 100)
  case x :: xs => {println(x); runT(xs)}
}
runT(xs)

List("A", "B", "C")
