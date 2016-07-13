package com.magnusjason.githubprstats

import org.apache.commons.io.input.CountingInputStream
import org.apache.http.Header
import org.apache.http.client.cache.HttpCacheEntry
import org.apache.http.client.cache.HttpCacheStorage
import org.apache.http.client.cache.HttpCacheUpdateCallback
import org.apache.http.message.BasicLineFormatter
import org.apache.http.message.BasicLineParser
import org.apache.http.message.BasicStatusLine
import scala.collection.mutable.MutableList
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.bind.DatatypeConverter

class FileHttpCacheStorage(val directory: Path) : HttpCacheStorage {

    val CRLF = "\r\n"

    val dateFormat = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)!!

    init{
        if(!Files.exists(directory)){
            Files.createDirectory(directory)
        }
    }

    override fun updateEntry(key: String, callback: HttpCacheUpdateCallback?) {
        callback.
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEntry(key: String): HttpCacheEntry? {
        return null
    }

    override fun putEntry(key: String, entry: HttpCacheEntry) {
        val path = directory.resolve(sha1Hash(key) + ".txt")
        serializeCacheEntry(key,entry,Files.newOutputStream(path))
    }

    override fun removeEntry(key: String) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun serializeField(key: String, value: String): ByteArray {
        return (key + ": " + value + CRLF).toByteArray()
    }

    fun deserializeField(field : String, delim: String = ": ") : Pair<String, String> {
        val delimIndex = field.indexOf(delim)
        val key = field.substring(0,delimIndex)
        val value = field.substring(delimIndex + 2)
        return Pair(key, value)
    }

    fun serializeCacheEntry(key: String, e: HttpCacheEntry, os: OutputStream){
        os.write(serializeField("URL", key))
        os.write(serializeField("RequestDate", dateFormat.format(e.requestDate.toInstant())))
        os.write(serializeField("ResponseDate", dateFormat.format(e.responseDate.toInstant())))
        e.variantMap.entries.forEach {
            os.write(serializeField("Variant", it.key + "=" + it.value))
        }
        os.write(CRLF.toByteArray())

        //val pver = e.statusLine.protocolVersion
        //os.write((pver.protocol + "/" + pver.major + "." + pver.minor +
        //        " " + e.statusCode + " " + e.statusLine.reasonPhrase + CRLF).toByteArray())
        os.write((BasicLineFormatter.formatStatusLine(e.statusLine, null) + CRLF).toByteArray())
        e.allHeaders.forEach {
            os.write(serializeField(it.name, it.value))
        }
        os.write(CRLF.toByteArray())
        e.resource.inputStream.copyTo(os)
    }

    fun deserializeCacheEntry(inputStream: InputStream) : HttpCacheEntry {
        //val countingInputStream = CountingInputStream(inputStream)
        val br = countingInputStream.bufferedReader()
        br.readLine()
        val requestDate = dateFormat.parse(deserializeField(br.readLine()).second)
        val responseDate = dateFormat.parse(deserializeField(br.readLine()).second)
        val variants = HashMap<String, String>()
        var line = br.readLine()
        while(!line.isEmpty()){
            val entry = deserializeField(deserializeField(line).second, "=")
            variants.put(entry.first, entry.second)
            line = br.readLine()
        }

        val statusLine = BasicLineParser.parseStatusLine(br.readLine(), null)
        val headers = ArrayList<Header>()
        line = br.readLine()
        while(!line.isEmpty()){
            headers.add(BasicLineParser.parseHeader(line, null))
            line = br.readLine()
        }
        countingInputStream.
                //TODO, work out position of data, account for over buffering then build Resource object

    }

    fun sha1Hash(key : String): String {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(key.toByteArray())
        return DatatypeConverter.printHexBinary(md.digest())
    }



}