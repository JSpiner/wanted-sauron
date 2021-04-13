import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import java.net.URL
import com.github.kittinunf.fuel.httpPost
import com.google.gson.reflect.TypeToken

fun main(args: Array<String>) {
    val jobSearchApiUrl = WANTED_JOB_SEARCH_API_TEMPLATE.format(ANDROID_CATEGORY)

    val rawResponse = URL(jobSearchApiUrl).readText()
    val response = Gson().fromJson(rawResponse, Response::class.java)

    val latestViewedJobId = Gson().fromJson<List<ViewData>>(
        LAST_VIEWED_ID_API_URL.httpGet()
            .header("x-apikey", System.getenv(ENV_KEY_REST_DB_KEY))
            .string(),
        object : TypeToken<List<ViewData>>() {}.type
    ).first().jobId
    val unCheckedCompanyList = response.data
        .takeWhile { it.id != latestViewedJobId }

    unCheckedCompanyList.onEach { data ->
        val message = ":bell: 띵동! 새로운 안드로이드 개발자 포지션이 생겼습니다.\n회사 : %s\n포지션 : %s\nhttps://www.wanted.co.kr/wd/%d"
            .format(data.company.name, data.position, data.id)

        System.getenv(ENV_KEY_SLACK_WEBHOOK).httpPost()
            .body(Gson().toJson(mapOf("text" to message)))
            .response()
    }.firstOrNull()
        ?.let {
            LAST_VIEWED_ID_API_URL.httpPost(listOf("jobId" to it.id))
                .header("x-apikey", System.getenv(ENV_KEY_REST_DB_KEY))
                .response()
        }

}

fun Request.string() = responseString().third.component1()

data class ViewData(val jobId: Int)

data class Response(val data: List<Data>)

data class Data(
    val id: Int,
    val position: String,
    val company: Company
)

data class Company(val name: String)

const val ANDROID_CATEGORY = 677
const val WANTED_JOB_SEARCH_API_TEMPLATE = "https://www.wanted.co.kr/api/v4/jobs?1617705029342&country=kr&tag_type_id=%d&job_sort=job.latest_order&locations=all&years=-1"
const val LAST_VIEWED_ID_API_URL = "https://wantedsauron-870f.restdb.io/rest/view?sort=_id&dir=-1"

const val ENV_KEY_SLACK_WEBHOOK = "SLACK_WEBHOOK"
const val ENV_KEY_REST_DB_KEY = "REST_DB_KEY"
