import org.jsoup.Jsoup.connect
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO


fun main(){
    println("Ввидите url страницы, чтобы начать анализ картинок")
    readLine()?.let{
        readImages(it)
    }
}

fun readImages (
    urlToConnect: String
){
    val imageList= mutableListOf<ImageInfo>()

    val domain = mutableListOf<Char>()
    var i = 3
    urlToConnect.toCharArray().forEach{
        if(i>0){
            domain.add(it)
            if(it=='/') i--
        }
    }

    val domainToUrl = StringBuilder().apply {
        for (i in 0 until domain.size){
            append(domain[i])
        }
    }.toString()

    val doc: Document = connect(urlToConnect).get()
    val imgFromSite: Elements = doc.getElementsByTag("img")

    getImagesToList(imageList, imgFromSite, domainToUrl)

    println("Найдено картинок - "+imageList.size)
    println("Информация о картинках:")
    imageList.forEachIndexed{index, img ->
        println("Картинка №${index+1}: $img")
    }


}

fun getFormat(url: String): String{
    val urlCharArray = url.toCharArray()
    return "${urlCharArray[url.length-3]}"+
            "${urlCharArray[url.length-2]}"+
            "${urlCharArray[url.length-1]}"
}

fun getResAndWeight(url: URL): ResolutionAndWeight {
    val baos = ByteArrayOutputStream()
    val iSt =url.openStream()
    val b = byteArrayOf(65536.toByte())
    var read: Int = iSt.read(b)
    while(read>-1) {
        baos.write(b,0,read)
        read = iSt.read(b)
    }
    val inBytes = baos.toByteArray().size
    val img = ImageIO.read(url)

    val weight = if(inBytes/1024 > 0) "${inBytes/1024} Кб"
    else "$inBytes Б"

    return ResolutionAndWeight(img.height,img.width, weight)
}

fun getImagesToList(imgList: MutableList<ImageInfo>, imgFromSite: Elements, domain: String): MutableList<ImageInfo> {
    imgFromSite.forEach { img ->
        var srcUrl = img.attr("src")

        if(srcUrl!=""){
            when(srcUrl.toCharArray()[0]){
                'h' -> if ("${srcUrl.toCharArray()[1]}" + "${srcUrl.toCharArray()[2]}" + "${srcUrl.toCharArray()[3]}" +"${ srcUrl.toCharArray()[4]}" == "ttp:") {
                    var newUrl = "https"
                    var i = 4
                    srcUrl.toCharArray().forEach {
                        if(i>0) i--
                        else newUrl+=it
                    }
                    srcUrl = newUrl
                }
                '.' -> {
                    val newUrl = StringBuilder().apply {
                        for (i in 2 until srcUrl.length)
                            append(srcUrl.toCharArray()[i])
                    }
                    srcUrl = domain + newUrl
                }

                '/' -> srcUrl = if (srcUrl.toCharArray()[1] == '.'){
                    val newUrl = StringBuilder().apply {
                        for (i in 3 until srcUrl.length)
                            append(srcUrl.toCharArray()[i])
                    }
                    domain + newUrl
                } else{
                    val newUrl = StringBuilder().apply {
                        for (i in 1 until srcUrl.length)
                            append(srcUrl.toCharArray()[i])
                    }
                    domain + newUrl
                }

                'p' -> srcUrl = domain + srcUrl
            }
            val url = URL(srcUrl)
            val format = getFormat(srcUrl)
            val resAndWeight = getResAndWeight(url)
            imgList.add(ImageInfo(url, format, resAndWeight.weight, resAndWeight.width, resAndWeight.height))
        }
    }

    return imgList
}

class ResolutionAndWeight(
    val width: Int,
    val height: Int,
    val weight: String
)

class ImageInfo (
    var url: URL,
    private var format: String,
    private var weight: String,
    private var width: Int,
    private var height: Int
){

    override fun toString(): String {
        return "URL-адрес - $url, формат - $format," +
                " размер ${width}x${height}, весит $weight"
    }
}