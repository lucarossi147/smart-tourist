package io.github.lucarossi147.smarttourist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.github.lucarossi147.smarttourist.Constants.ARG_USER
import io.github.lucarossi147.smarttourist.data.model.LoggedInUser
import io.github.lucarossi147.smarttourist.data.model.Signature
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

/**
 * A simple [Fragment] subclass.
 * Use the [SignaturesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SignaturesFragment : Fragment() {

    private var user: LoggedInUser? = null
    private var poiId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            user = it.getParcelable(ARG_USER)
            poiId = it.getString("poiId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_signatures, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            val res =
                HttpClient(Android).get(Constants.getSignatures(poiId!!)) {
                    bearerAuth(user!!.token)
                }
            if (res.status.isSuccess()) {
                val signatures = Gson().fromJson(res.bodyAsText(),Array<Signature>::class.java).toList()
                CoroutineScope(Dispatchers.Main).launch {
                    val signatureRecyclerView: RecyclerView = view.findViewById(R.id.signatureRecyclerView)!!
                    val signatureAdapter = SignatureAdapter(signatures)
                    signatureRecyclerView.adapter = signatureAdapter
                    signatureRecyclerView.layoutManager = LinearLayoutManager(context)
                }
            }
        }
    }
}