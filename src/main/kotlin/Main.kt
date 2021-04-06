import com.google.gson.Gson
import java.net.URL
import com.github.kittinunf.fuel.httpPost

fun main(args: Array<String>) {
    val jobSearchApiUrl = WANTED_JOB_SEARCH_API_TEMPLATE.format(ANDROID_CATEGORY)

    val rawResponse = URL(jobSearchApiUrl).readText()
    val response = Gson().fromJson(rawResponse, Response::class.java)

    val latestViewedCompanyId = System.getenv(ENV_KEY_LATEST_VIEW_COMPANY_ID)?.toIntOrNull() ?: -1
    val unCheckedCompanyList = response.data
        .takeWhile { it.company.id != latestViewedCompanyId }

    unCheckedCompanyList.onEach { data ->
        val message = ":bell: 띵동! 새로운 안드로이드 개발자 포지션이 생겼습니다.\n회사 : %s\n포지션 : %s\nhttps://www.wanted.co.kr/wd/%d"
            .format(data.company.name, data.position, data.company.id)

        System.getenv(ENV_KEY_SLACK_WEBHOOK).httpPost()
            .body(Gson().toJson(mapOf("text" to message)))
            .response()
    }.lastOrNull()
        ?.let { Runtime.getRuntime().exec("$ENV_KEY_LATEST_VIEW_COMPANY_ID=${it.company.id}") }

}

data class Response(val data: List<Data>)

data class Data(
    val position: String,
    val company: Company
)

data class Company(
    val id: Int,
    val name: String
)

const val ANDROID_CATEGORY = 677
const val WANTED_JOB_SEARCH_API_TEMPLATE = "https://www.wanted.co.kr/api/v4/jobs?1617705029342&country=kr&tag_type_id=%d&job_sort=job.latest_order&locations=all&years=-1"

const val ENV_KEY_SLACK_WEBHOOK = "SLACK_WEBHOOK"
const val ENV_KEY_LATEST_VIEW_COMPANY_ID = "LATEST_VIEW_COMPANY_ID"
