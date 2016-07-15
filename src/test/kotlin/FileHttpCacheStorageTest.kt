import com.google.common.jimfs.Jimfs
import com.magnusjason.githubprstats.FileHttpCacheStorage
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths


class FileHttpCacheStorageTest {

    val jimfs = Jimfs.newFileSystem()


    //TODO create some anonymized test data
    val key = ""

    val filePath = "69ADAFE12F64EF6DE6A6C6791B81922BEB4D76FD.txt"


    //@Test
    fun deserializeSerializeMatchTest(){
        val testDir = Paths.get("src", "test", "resources")
        val cache = FileHttpCacheStorage(testDir)
        val entry = cache.getEntry(key)

        val tempCache = FileHttpCacheStorage(jimfs.getPath(""))
        tempCache.putEntry(key, entry!!)

        val originalBytes = Files.readAllBytes(testDir.resolve(filePath))
        val generatedBytes = Files.readAllBytes(jimfs.getPath(filePath))

        Assert.assertArrayEquals(originalBytes, generatedBytes)

    }
}