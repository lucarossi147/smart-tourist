package io.github.lucarossi147.smarttourist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.lucarossi147.smarttourist.data.model.Signature

class SignatureAdapter(private val signatures: List<Signature>):RecyclerView.Adapter<SignatureAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val usernameTextView: TextView = view.findViewById(R.id.signatureUsername)
        val messageTextView: TextView = view.findViewById(R.id.signatureMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignatureAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.signature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SignatureAdapter.ViewHolder, position: Int) {
        holder.usernameTextView.text = signatures[position].username
        holder.messageTextView.text = signatures[position].message

    }

    override fun getItemCount(): Int  = signatures.size

}