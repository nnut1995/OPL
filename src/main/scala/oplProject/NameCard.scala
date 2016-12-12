package oplProject

import org.scalatra._
import org.scalatra.ScalatraServlet
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import org.bytedeco.javacpp.tesseract
import org.bytedeco.javacpp.lept._
import java.io._
import slick.driver.JdbcDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatra.{ScalatraBase, FutureSupport, ScalatraServlet}
import scala.util.matching.Regex
import scala.io.Source.fromFile

object GlobalVar {
  var id = 1
}

object Tables {

  // Name Card Class
  class Cards(tag: Tag) extends Table[(Int,String, String, String, String, String)](tag, "Card") {
    def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
    def name = column[String]("SUP_NAME")
    def email = column[String]("EMAIL")
    def website = column[String]("WEBSITE")
    def phoneNumber = column[String]("PHONENUMBER")
    def whole_text = column[String]("TEXT")

    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, name, email, website, phoneNumber, whole_text)
  }

  val card = TableQuery[Cards]

  // back up for search
  // type cardTuple = (Int,String,String,String,String)
  // val search = (res:Vector[cardTuple],in:String) => {
  //   def searchAll(v:Vector[cardTuple],key:String,ans:Vector[cardTuple]): Vector[cardTuple] = {
  //     if(v.isEmpty) ans
  //     else {
  //       if (Vector(v.head._1, v.head._2, v.head._3, v.head._4, v.head._5).contains(in)) searchAll(v.tail,key,ans:+v.head)
  //       else searchAll(v.tail,key,ans)
  //     }
  //   }
  //   searchAll(res,in,Vector())
  // }

  val search = (in: String) => {
    val lower_in = in.toLowerCase
    val q1 = card.filter(_.name.toLowerCase === lower_in)
    val q2 = card.filter(_.email.toLowerCase === lower_in)
    val q3 = card.filter(_.website.toLowerCase === lower_in)
    val q4 = card.filter(_.phoneNumber.toLowerCase === lower_in)

    val unionQ = (q1 union q2) union (q3 union q4)
    unionQ.result
  }
  val findAll = card.result

  val insert = (id:Int,name: String, email: String, website: String, phoneNumber: String, text: String) => {
    DBIO.seq(Tables.card += (id,name,email,website,phoneNumber,text))
  }

  val initTable = DBIO.seq(
    Tables.card += (0, "Sample Name", "Sample Email", "Sample Website", "Sample PhoneNumber", "Sample Card Text")
  )

  // DBIO Action which creates the schema
  val createSchemaAction = (card.schema).create
  // DBIO Action which drops the schema
  val dropSchemaAction = (card.schema).drop
  // Create database, composing create schema and insert sample data actions
  val createDatabase = DBIO.seq(createSchemaAction, initTable)

}


trait SlickRoutes extends ScalatraBase with FutureSupport {

  def db: Database

  get("/db/create-db") {
    db.run(Tables.createDatabase)
  }

  get("/db/drop-db") {
    db.run(Tables.dropSchemaAction)
  }

  get("/myCard"){
    db.run(Tables.findAll) map { xs =>
      contentType = "text/plain"
      xs map {
        case (id,s1,s2,s3,s4,s5) => f"ID: $id \n Name: $s1 \n Email: $s2 \n Website: $s3 \n Phone Number: $s4 \n Card Text:\n\n$s5"
      } mkString "\n\n"
    }
  }

  post("/search"){
    val input = params("search")
    println(input)
    db.run(Tables.search(input)) map { xs =>
      contentType = "text/plain"
      xs map {
        case (id,s1,s2,s3,s4,s5) => f"ID: $id \n Name: $s1 \n Email: $s2 \n Website: $s3 \n Phone Number: $s4 \n Card Text: \n$s5"
      } mkString "\n\n"
    }
  }
}

class NameCard(val db: Database) extends ScalatraServlet with FileUploadSupport with FlashMapSupport with FutureSupport with SlickRoutes {

  protected implicit def executor = scala.concurrent.ExecutionContext.Implicits.global

  db.run(Tables.createDatabase)

  get("/") {
    contentType="text/html"
    new java.io.File(servletContext.getResource("/html/home.html").getFile)
  }

  get("/search"){
    contentType="text/html"
    new java.io.File(servletContext.getResource("/html/search.html").getFile)
  }


  get("/upload"){
    contentType="text/html"
    new java.io.File(servletContext.getResource("/html/upload.html").getFile)
  }

  post("/upload"){
    fileParams.get("file") match {
        case Some(file) => {
          val a = file.get()
          val bos = new BufferedOutputStream(new FileOutputStream(file.name))
          Stream.continually(bos.write(a))
          bos.close()
          println("file created")
          val TESSDATA_PREFIX = "data/tesseract-ocr-3.02/"
          val lang = "eng"
          val path = file.name
          val t = tesseract.TessBaseAPICreate
          val rc = tesseract.TessBaseAPIInit3(t, TESSDATA_PREFIX, lang)
          if (rc != 0) {
            tesseract.TessBaseAPIDelete(t)
            println("Init failed")
            sys.exit(3)
          }
          val image = pixRead(path)
          t.SetImage(image)
          t.GetUTF8Text.getString
          new PrintWriter("Ans.txt") { write(t.GetUTF8Text.getString); close }
          t.End
          pixDestroy(image)

          contentType="text/html"
          new java.io.File(servletContext.getResource("/html/afterUpload.html").getFile)
        }
        case None => <p> No file uploaded </p>
      }
    }

  get("/database"){
    val regex_name = "/^[a-z ,.'-]+$/i".r
    val regex_email = """(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b""".r
    val regex_website = "^(\\w+):\\/{2}(\\w+)\\.([^\\/]+)([^\\?]+)\\?([^&]+)&([^&]+)&([^&]+)".r
    val regex_phoneNumber = """([0-9]{1,3})[ -]([0-9]{1,3})[ -]([0-9]{4,10})""".r
    val whole_text = scala.io.Source.fromFile("Ans.txt").mkString
    val nameList = fromFile("./Ans.txt").getLines.flatMap { regex_name.findAllIn _ }.toList
    val emailList = fromFile("./Ans.txt").getLines.flatMap { regex_email.findAllIn _ }.toList
    val websiteList = fromFile("./Ans.txt").getLines.flatMap { regex_website.findAllIn _ }.toList
    val phoneNumberList = fromFile("./Ans.txt").getLines.flatMap { regex_phoneNumber.findAllIn _ }.toList


    val combineList = nameList::emailList::websiteList::phoneNumberList::Nil

    def checkIfEmpty(input: List[List[String]], ans: List[String]): List[String] = {
      if (input.isEmpty) ans
      else {
        if (input.head.isEmpty) checkIfEmpty(input.tail,"No entry"::ans)
        else checkIfEmpty(input.tail,input.head.head::ans)
      }
    }

    val afterCheckList = checkIfEmpty(combineList,Nil).reverse

    println(afterCheckList)
    db.run(Tables.insert(GlobalVar.id,afterCheckList(0),afterCheckList(1),afterCheckList(2),afterCheckList(3),whole_text))
    // increment id each time so it is unique for each databse insertion
    GlobalVar.id+=1

    contentType="text/html"
    new java.io.File(servletContext.getResource("/html/database.html").getFile)
  }

}
