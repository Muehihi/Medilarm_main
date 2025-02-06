package com.example.medilarm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUp : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance() // Initialize Firestore

        val firstNameInput = findViewById<EditText>(R.id.gName_input)
        val lastNameInput = findViewById<EditText>(R.id.surname_input)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val confirmPasswordInput = findViewById<EditText>(R.id.Confirm_password_input)
        val signUpButton = findViewById<Button>(R.id.btn_signIn)

        signUpButton.setOnClickListener {
            val fname = firstNameInput.text.toString().trim()
            val lname = lastNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (fname.isNotEmpty() && lname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = firebaseAuth.currentUser?.uid
                                if (uid != null) {
                                    val userMap = hashMapOf(
                                        "fname" to fname,
                                        "lname" to lname,
                                        "email" to email
                                    )

                                    // Use Firestore to store user data
                                    firestore.collection("users").document(uid).set(userMap)
                                        .addOnCompleteListener { dbTask ->
                                            if (dbTask.isSuccessful) {
                                                Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                                                startActivity(Intent(this, LogIn::class.java))
                                                finish()
                                            } else {
                                                Toast.makeText(this, "Firestore Error: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            } else {
                                Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }

        val loginRedirectText = findViewById<TextView>(R.id.linkToSignUp)
        loginRedirectText.setOnClickListener {
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
    }
}