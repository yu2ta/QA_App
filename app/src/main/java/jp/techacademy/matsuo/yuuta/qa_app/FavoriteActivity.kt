package jp.techacademy.matsuo.yuuta.qa_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_favorite.*

class FavoriteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        val mDatabaseReference = FirebaseDatabase.getInstance().reference
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val favRef = mDatabaseReference.child(FavoritePATH).child(uid)

        var qList = ArrayList<String>()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, qList)
        listView.adapter = adapter

        favRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val data_list = dataSnapshot.value as Map<*, *>
                for(data in data_list) {
                    var p = data.value as Map<*,*>
                    qList.add(p.get("title").toString())
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {}

        })
    }
}