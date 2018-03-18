package com.anmol.coinpanda.Fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anmol.coinpanda.Adapters.GridnewAdapter
import com.anmol.coinpanda.Interfaces.ItemClickListener
import com.anmol.coinpanda.Model.Allcoin
import com.anmol.coinpanda.R
import com.anmol.coinpanda.TweetsActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Created by anmol on 3/15/2018.
 */
class mycoinslist : Fragment(){
    private var coingrid: GridView?=null
    private lateinit var gridAdapter: GridnewAdapter
    lateinit var allcoins:MutableList<Allcoin>
    lateinit var itemClickListener : ItemClickListener
    var db = FirebaseFirestore.getInstance()
    var empty: TextView? = null
    val auth = FirebaseAuth.getInstance()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val vi = inflater.inflate(R.layout.mycoinslist, container, false)
        coingrid = vi.findViewById(R.id.coingrid)
        empty = vi.findViewById(R.id.empty)
        empty?.visibility = View.GONE
        allcoins = ArrayList()
        val handler = Handler()
        handler.postDelayed({
            loaddata()
        },200)

        coingrid?.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            if(activity!=null){
                val dialog = Dialog(activity)
                dialog.setContentView(R.layout.dialoglayout)
                val prg : ProgressBar? = dialog.findViewById(R.id.prgbr)
                val coinimg: ImageView? = dialog.findViewById(R.id.coinimg)
                val cn :TextView? = dialog.findViewById(R.id.cn)
                val cs:TextView? = dialog.findViewById(R.id.cs)
                val cp:TextView? = dialog.findViewById(R.id.cp)
                val atp:Button? = dialog.findViewById(R.id.atp)
                val portfoliolay: LinearLayout?= dialog.findViewById(R.id.portfoliolay)
                val viewtweet:Button?=dialog.findViewById(R.id.viewtweets)
                val notificationswitch: Switch?= dialog.findViewById(R.id.notification)
                val remove:Button? = dialog.findViewById(R.id.remove)
                atp?.visibility = View.VISIBLE
                portfoliolay?.visibility = View.GONE
                db.collection("users").document(auth.currentUser!!.uid).collection("portfolio").document(allcoins[i].coinname!!).get().addOnCompleteListener { task ->
                    val snapshot = task.result
                    if(snapshot.exists()){
                        val notify = snapshot?.getBoolean("notify")
                        if (notify!!){
                            notificationswitch?.isChecked = true
                        }
                    }

                }
                notificationswitch?.setOnCheckedChangeListener { _, b ->
                    if (b){
                        val map = HashMap<String,Any>()
                        map["notify"] = true
                        topicsearch(0,allcoins[i].coinname)
                        db.collection("users").document(auth.currentUser!!.uid).collection("portfolio").document(allcoins[i].coinname!!).update(map)
                    }
                    else{
                        db.collection("users").document(auth.currentUser!!.uid).collection("topics").get().addOnCompleteListener {task->
                            val documentSnapshot = task.result
                            for(doc in documentSnapshot){
                                if(doc.id.contains(allcoins[i].coinname!!)){
                                    removetopic(doc.id)
                                }
                            }
                        }
                        val map = HashMap<String,Any>()
                        map["notify"] = false
                        db.collection("users").document(auth.currentUser!!.uid).collection("portfolio").document(allcoins[i].coinname!!).update(map)
                    }
                }
                db.collection("users").document(auth.currentUser!!.uid).collection("portfolio").get().addOnCompleteListener {task ->
                    for(doc in task.result){
                        if(doc.id == allcoins[i].coinname!!){
                            atp?.visibility = View.GONE
                            portfoliolay?.visibility = View.VISIBLE
                        }
                    }
                }
                cn?.text = allcoins[i].coin
                cs?.text = allcoins[i].coinname
                cp?.text = "#"+ allcoins[i].coinpage
                val urlpng = "https://raw.githubusercontent.com/crypti/cryptocurrencies/master/images/"+ allcoins[i].coinname+".png"
                val urljpg = "https://raw.githubusercontent.com/crypti/cryptocurrencies/master/images/"+ allcoins[i].coinname+".jpg"
                val urljpeg = "https://raw.githubusercontent.com/crypti/cryptocurrencies/master/images/"+ allcoins[i].coinname+".jpeg"
                if(activity!=null){
                    Glide.with(activity).load(urlpng).listener(object : RequestListener<Drawable> {
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {

                            return false
                        }

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            Glide.with(activity).load(urljpg).listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                    Glide.with(activity).load(urljpeg).into(coinimg)
                                    return true
                                }

                                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                    return false
                                }

                            }).into(coinimg)
                            return true
                        }

                    }).into(coinimg)

                }

                viewtweet?.setOnClickListener {
                    if(activity!=null){
                        val intent = Intent(activity, TweetsActivity::class.java)
                        intent.putExtra("coin", allcoins[i].coinname)
                        startActivity(intent)
                    }

                }
                remove?.setOnClickListener {
                    db.collection("users").document(auth.currentUser!!.uid).collection("topics").get().addOnCompleteListener {task->
                        val documentSnapshot = task.result
                        for(doc in documentSnapshot){
                            if(doc.id.contains(allcoins[i].coinname!!)){
                                removetopic(doc.id)
                            }
                        }
                    }
                    db.collection("users").document(auth.currentUser!!.uid).collection("portfolio").document(allcoins[i].coinname!!)
                            .delete().addOnSuccessListener {
                                if(activity!=null){
                                    Toast.makeText(activity,"Removed from your Portfolio", Toast.LENGTH_SHORT).show()
                                }

                            }
                    dialog.dismiss()

                }
                atp?.setOnClickListener {
                    //                val intent = Intent(activity,PaymentActivity::class.java)
//                intent.putExtra("coin",allcoins[i].coin)
//                startActivity(intent)
                    topicsearch(0,allcoins[i].coinname)
                    prg?.visibility = View.VISIBLE
                    atp.visibility = View.GONE
                    val map = HashMap<String,Any>()
                    map["coin_name"] = allcoins[i].coin.toString()
                    map["coinPage"] = allcoins[i].coinpage.toString()
                    map["notify"] = true
                    db.collection("users").document(auth.currentUser!!.uid).collection("portfolio").document(allcoins[i].coinname!!).set(map).addOnSuccessListener {
                        if(activity!=null){
                            Toast.makeText(activity,"Added to your Portfolio", Toast.LENGTH_SHORT).show()
                        }


                    }
                    topicsearch(0,allcoins[i].coinname)
                    dialog.dismiss()
                }
                dialog.show()
            }

        }
        return vi

    }
    private fun removetopic(id: String) {
        db.collection("topics").document(id).get().addOnCompleteListener{task ->
            val documentSnapshot = task.result
            val count : Int = documentSnapshot.get("count") as Int
            if (count>0){
                val map  = java.util.HashMap<String, Any>()
                map["count"] = count - 1
                db.collection("topics").document(id).set(map)
            }
            else{

            }
        }
        db.collection("users").document(auth.currentUser!!.uid).collection("topics").document(id).delete()
    }
    private fun topicsearch(i: Int, coinname: String?) {
        db.collection("topics").document(coinname + i.toString()).get().addOnCompleteListener { task->
            val documentSnapshot = task.result
            if(documentSnapshot.exists()){
                val count : Int = documentSnapshot.get("count") as Int
                if(count > 990){
                    topicsearch(i+1,coinname)
                }
                else{
                    val map  = HashMap<String,Any>()
                    map["notify"] = true
                    db.collection("users").document(auth.currentUser!!.uid).collection("topics").document(coinname + i.toString())
                            .set(map).addOnSuccessListener {
                                val message = FirebaseMessaging.getInstance()
                                message.subscribeToTopic(coinname + i.toString())
                            }
                }
            }
            else{
                val map  = HashMap<String,Any>()
                map["notify"] = true
                db.collection("users").document(auth.currentUser!!.uid).collection("topics").document(coinname + i.toString())
                        .set(map).addOnSuccessListener {
                            val message = FirebaseMessaging.getInstance()
                            message.subscribeToTopic(coinname + i.toString())
                            val count = HashMap<String,Any>()
                            count["count"] = i+1
                            db.collection("topics").document(coinname + i.toString()).set(count)
                        }
            }

        }
    }

    private fun loaddata() {
        allcoins.clear()
        db.collection("users").document(auth.currentUser!!.uid).collection("portfolio").addSnapshotListener{documentSnapshot, e ->
            allcoins.clear()
            for(doc in documentSnapshot.documents){
                val coinname = doc.id
                val name = doc.getString("coin_name")
                val coinpage = doc.getString("coinPage")
                val allcoin = Allcoin(coinname,name,coinpage)
                allcoins.add(allcoin)
            }
            if(activity!=null){
                if(!allcoins.isEmpty()){
                    empty?.visibility = View.GONE
                    gridAdapter = GridnewAdapter(activity!!, allcoins)
                    gridAdapter.notifyDataSetChanged()
                    coingrid?.adapter = gridAdapter
                }
                else{
                    empty?.visibility = View.VISIBLE
                }
            }


        }
    }

}