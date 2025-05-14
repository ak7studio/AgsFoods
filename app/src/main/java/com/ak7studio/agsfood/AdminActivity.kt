package com.ak7studio.agsfood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<User>()

    private lateinit var whitelistRecyclerView: RecyclerView
    private lateinit var whitelistAdapter: WhitelistAdapter
    private val whitelistEmails = mutableListOf<String>()

    private lateinit var etNewWhitelistEmail: EditText
    private lateinit var btnAddWhitelistEmail: Button

    private val database = FirebaseDatabase.getInstance("https://agsfoods-d6f62-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    override fun getCurrentNavItemId(): Int = R.id.nav_admin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentFrameLayout = findViewById<FrameLayout>(R.id.content_frame)
        LayoutInflater.from(this).inflate(R.layout.activity_admin, contentFrameLayout, true)

        // Only admin can access
        if (UserPrefs.getRole() != "admin") {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup users RecyclerView
        recyclerView = findViewById(R.id.usersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UserAdapter(userList)
        recyclerView.adapter = adapter

        // Setup whitelist RecyclerView
        whitelistRecyclerView = findViewById(R.id.whitelistRecyclerView)
        whitelistRecyclerView.layoutManager = LinearLayoutManager(this)
        whitelistAdapter = WhitelistAdapter(whitelistEmails)
        whitelistRecyclerView.adapter = whitelistAdapter

        etNewWhitelistEmail = findViewById(R.id.etNewWhitelistEmail)
        btnAddWhitelistEmail = findViewById(R.id.btnAddWhitelistEmail)

        btnAddWhitelistEmail.setOnClickListener {
            val email = etNewWhitelistEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter an email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Add Whitelist Email")
                .setMessage("Are you sure you want to add \"$email\" to the whitelist?")
                .setPositiveButton("Yes") { _, _ -> addWhitelistEmail(email) }
                .setNegativeButton("No", null)
                .show()
        }


        loadUsers()
        loadWhitelistEmails()
    }

    private fun loadUsers() {
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        it.uid = userSnapshot.key ?: ""
                        userList.add(it)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminActivity, "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadWhitelistEmails() {
        database.child("whitelist_emails").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                whitelistEmails.clear()
                for (child in snapshot.children) {
                    val emailKey = child.key ?: continue
                    whitelistEmails.add(decodeEmail(emailKey))
                }
                whitelistAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminActivity, "Failed to load whitelist emails: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun addWhitelistEmail(email: String) {
        val encodedEmail = encodeEmail(email)
        database.child("whitelist_emails").child(encodedEmail).setValue(true).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Email added to whitelist", Toast.LENGTH_SHORT).show()
                etNewWhitelistEmail.text.clear()
                loadWhitelistEmails()
            } else {
                Toast.makeText(this, "Failed to add email: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeWhitelistEmail(email: String) {
        val encodedEmail = encodeEmail(email)
        database.child("whitelist_emails").child(encodedEmail).removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Email removed from whitelist", Toast.LENGTH_SHORT).show()
                loadWhitelistEmails()
            } else {
                Toast.makeText(this, "Failed to remove email: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    inner class UserAdapter(private val users: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
            val tvUserId: TextView = itemView.findViewById(R.id.tvUserId)
            val tvUserRole: TextView = itemView.findViewById(R.id.tvUserRole)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_user_card, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]
            holder.tvUserName.text = user.username
            holder.tvUserId.text = "ID: ${user.uid}"
            holder.tvUserRole.text = "Role: ${user.role}"

            holder.itemView.setOnClickListener {
                showEditUserDialog(user)
            }
        }

        override fun getItemCount() = users.size
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_user_edit, null)
        val etUsername = dialogView.findViewById<EditText>(R.id.etDialogUsername)
        val tvUserId = dialogView.findViewById<TextView>(R.id.tvDialogUserId)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tvDialogEmail)
        val spRole = dialogView.findViewById<Spinner>(R.id.spDialogRole)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnDialogSubmit)

        etUsername.setText(user.username)
        tvUserId.text = "User ID: ${user.uid}"
        tvEmail.text = user.email

        val roles = arrayOf("cashier", "manager", "admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = adapter
        spRole.setSelection(roles.indexOf(user.role))

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSubmit.setOnClickListener {
            val newUsername = etUsername.text.toString().trim()
            val newRole = spRole.selectedItem.toString()

            if (newUsername.isEmpty()) {
                etUsername.error = "Username cannot be empty"
                return@setOnClickListener
            }

            // Show confirmation popup
            AlertDialog.Builder(this)
                .setTitle("Confirm Update")
                .setMessage("Change username to \"$newUsername\" and role to \"$newRole\"?")
                .setPositiveButton("Yes") { _, _ ->
                    updateUser(user, newUsername, newRole)
                    alertDialog.dismiss()
                }
                .setNegativeButton("No", null)
                .show()
        }

        alertDialog.show()
    }

    private fun updateUser(user: User, newUsername: String, newRole: String) {
        // Only update if changed
        if (newUsername != user.username) {
            updateUsername(user, newUsername)
        }
        if (newRole != user.role) {
            updateRole(user, newRole)
        }
    }


    inner class WhitelistAdapter(private val emails: List<String>) : RecyclerView.Adapter<WhitelistAdapter.EmailViewHolder>() {

        inner class EmailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val emailText: TextView = itemView.findViewById(R.id.tvWhitelistEmail)
            val btnRemove: View = itemView.findViewById(R.id.btnRemoveWhitelistEmail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_whitelist_email, parent, false)
            return EmailViewHolder(view)
        }

        override fun onBindViewHolder(holder: EmailViewHolder, position: Int) {
            val email = emails[position]
            holder.emailText.text = email
            holder.btnRemove.setOnClickListener {
                AlertDialog.Builder(this@AdminActivity)
                    .setTitle("Remove Email")
                    .setMessage("Are you sure you want to remove \"$email\" from the whitelist?")
                    .setPositiveButton("Yes") { _, _ -> removeWhitelistEmail(email) }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

        override fun getItemCount() = emails.size
    }

    private fun updateUsername(user: User, newUsername: String) {
        if (newUsername == user.username) return

        AlertDialog.Builder(this)
            .setTitle("Update Username")
            .setMessage("Are you sure you want to change username from \"${user.username}\" to \"$newUsername\"?")
            .setPositiveButton("Yes") { _, _ ->
                database.child("usernames").child(newUsername).get().addOnSuccessListener {
                    if (it.exists()) {
                        Toast.makeText(this, "Username already exists!", Toast.LENGTH_SHORT).show()
                    } else {
                        val updates = hashMapOf<String, Any>(
                            "users/${user.uid}/username" to newUsername,
                            "usernames/$newUsername" to user.email
                        )
                        database.child("usernames").child(user.username).removeValue()
                        database.updateChildren(updates).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Update failed: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateRole(user: User, newRole: String) {
        if (newRole == user.role) return

        AlertDialog.Builder(this)
            .setTitle("Update Role")
            .setMessage("Are you sure you want to change role from \"${user.role}\" to \"$newRole\"?")
            .setPositiveButton("Yes") { _, _ ->
                database.child("users").child(user.uid).child("role").setValue(newRole)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this, "Role updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Update failed: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun encodeEmail(email: String): String {
        return email.replace(".", ",")
    }

    private fun decodeEmail(encoded: String): String {
        return encoded.replace(",", ".")
    }

    data class User(
        var uid: String = "",
        val email: String = "",
        var username: String = "",
        var role: String = "cashier"
    )
}
