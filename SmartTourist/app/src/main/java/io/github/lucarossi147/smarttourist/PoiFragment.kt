package io.github.lucarossi147.smarttourist

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.github.lucarossi147.smarttourist.data.model.Signature
import io.github.lucarossi147.smarttourist.data.model.Token
import kotlin.random.Random

private const val ARG_POI = "poi"

/**
 * A simple [Fragment] subclass.
 * Use the [PoiFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PoiFragment : Fragment() {
    private var poi: POI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            poi = it.getParcelable(ARG_POI)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_poi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv: TextView = view.findViewById(R.id.poiInfoTextView)
        //TODO: put in a layout editText and button and show them only if user hasn't signed himself yet
        // TODO: fetch poi info
        tv.text = resources.getString(R.string.loremIpsum)
        val signButton: Button = view.findViewById(R.id.signButton)
        signButton.setOnClickListener {
            // TODO: send signature and comment to server
            view.findNavController().navigate(R.id.mapsFragment)
        }
        val goToSignatureButton: Button = view.findViewById(R.id.goToSignaturesButton)
        goToSignatureButton.setOnClickListener {
            // TODO: maybe ask to server for signature asyncronously in the onCreate and make
            //  fragment take a list as argument so user dosen´t have to wait
            view.findNavController().navigate(R.id.signaturesFragment)
        }
        repeat(20){
            val iv = ImageView(context)
            val linearLayout: LinearLayout = view.findViewById(R.id.images_linear_layout)
            linearLayout.addView(iv)
            val r = Random.nextInt(500)
            Picasso.get()
                .load("https://placedog.net/$r")
                .resize(0, 400)
                .into(iv)
        }
    }
    companion object {
        /**
         * @param param1 Parameter 1.
         * @return A new instance of fragment PoiFragment.
         */
        @JvmStatic
        fun newInstance(poi: POI) =
            PoiFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_POI, poi)
                }
            }
    }
}