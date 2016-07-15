package com.magnusjason.githubprstats

import org.apache.http.Header
import org.apache.http.client.cache.HttpCacheEntry
import org.apache.http.client.cache.HttpCacheStorage
import org.apache.http.client.cache.HttpCacheUpdateCallback
import org.apache.http.client.cache.Resource
import org.apache.http.message.*
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.bind.DatatypeConverter

/**
 * Implementation of HttpCacheStorage that stores cached resources as flat files in a HTTP style encoding.
 * File names are a SHA-1 hash of the key/url.
 *
 * The goal of this storage implementation is to have a persistent human-readable cache.
 * Consider disabling compression on the client if you want to be able to read response bodies.
 *
 * Using the bundled heap or ehcache implementations should be much faster than this implementation.
 */
class FileHttpCacheStorage(val directory: Path) : HttpCacheStorage {

    val CRLF = "\r\n"

    val dateFormat = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)!!

    init{
        if(!Files.exists(directory)){
            Files.createDirectory(directory)
        }
    }

    override fun updateEntry(key: String, callback: HttpCacheUpdateCallback) {
        putEntry(key, callback.update(getEntry(key)))
    }

    override fun getEntry(key: String): HttpCacheEntry? {
        val path = keyToPath(key)
        if(Files.exists(path)) {
            return deserializeCacheEntry(path)
        } else {
            return null
        }
    }

    override fun putEntry(key: String, entry: HttpCacheEntry) {
        val path = keyToPath(key)
        serializeCacheEntry(key, entry, Files.newOutputStream(path))
    }

    override fun removeEntry(key: String) {
        Files.delete(keyToPath(key))
    }

    fun serializeField(key: String, value: String): ByteArray {
        return (key + ": " + value + CRLF).toByteArray()
    }

    fun deserializeField(field : String, delim: String = ": ") : Pair<String, String> {
        val delimIndex = field.indexOf(delim)
        val key = field.substring(0,delimIndex)
        val value = field.substring(delimIndex + delim.length)
        return Pair(key, value)
    }

    fun serializeCacheEntry(key: String, e: HttpCacheEntry, os: OutputStream){
        os.use {
            os.write(serializeField("URL", key))
            os.write(serializeField("RequestDate", dateFormat.format(e.requestDate.toInstant())))
            os.write(serializeField("ResponseDate", dateFormat.format(e.responseDate.toInstant())))
            os.write(serializeField("ResourceLength", ""+e.resource.length()))

            e.variantMap.entries.forEach {
                os.write(serializeField("Variant", URLEncoder.encode(it.key, "UTF-8") + "=" + it.value))
            }
            os.write(CRLF.toByteArray())
            os.write((BasicLineFormatter.formatStatusLine(e.statusLine, null) + CRLF).toByteArray())
            e.allHeaders.forEach {
                os.write(serializeField(it.name, it.value))
            }
            os.write(CRLF.toByteArray())
            e.resource.inputStream.use {
                it.copyTo(os)
            }
        }
    }

    fun parseDate(date: String) : Date{
        return Date(Instant.from(dateFormat.parse(deserializeField(date).second)).toEpochMilli())
    }

    fun deserializeCacheEntry(path: Path) : HttpCacheEntry {
        Files.newInputStream(path).use { inputstream->
            val br = inputstream.bufferedReader()
            br.readLine()
            val requestDate = parseDate(br.readLine())
            val responseDate = parseDate(br.readLine())
            val resourceLength = deserializeField(br.readLine()).second.toLong()
            val variants = HashMap<String, String>()
            var line = br.readLine()
            while(!line.isEmpty()){
                val entry = deserializeField(deserializeField(line).second, "=")
                variants.put(URLDecoder.decode(entry.first, "UTF-8"), entry.second)
                line = br.readLine()
            }

            val statusLine = BasicLineParser.parseStatusLine(br.readLine(), null)
            val headers = ArrayList<Header>()
            line = br.readLine()
            while(!line.isEmpty()){
                headers.add(BasicLineParser.parseHeader(line, null))
                line = br.readLine()
            }

            val resource = SimpleResource(resourceLength, path, Files.size(path) - resourceLength)
            return HttpCacheEntry(requestDate, responseDate, statusLine, headers.toTypedArray(), resource, variants)
        }
    }

    fun keyToPath(key: String): Path {
        return directory.resolve(sha1Hash(key) + ".txt")
    }

    fun sha1Hash(key : String): String {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(key.toByteArray())
        return DatatypeConverter.printHexBinary(md.digest())
    }

    class SimpleResource(val length : Long, val path: Path, val offset: Long) : Resource {

        override fun getInputStream(): InputStream {
            val resourceStream = Files.newInputStream(path)
            resourceStream.skip(offset)
            return resourceStream
        }

        override fun length(): Long {
            return length
        }

        override fun dispose() {
            //not implemented
        }

    }


}