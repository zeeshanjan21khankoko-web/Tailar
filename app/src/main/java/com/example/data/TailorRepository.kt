package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Customer
import com.example.model.LanguageMode
import com.example.model.MeasurementHistory
import com.example.model.MeasurementUnit
import com.example.model.ShalwarKameezMeasurement
import com.example.model.ShopProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TailorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthenticatedUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    fun observeShopProfile(userId: String): Flow<ShopProfile?> = flow {
        val uid = requireAuthenticatedUser(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        ShopProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveShopProfile(
        userId: String,
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        preferredUnit: MeasurementUnit,
        preferredLanguage: LanguageMode
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireAuthenticatedUser(userId)
            val cleanName = shopName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Shop name is required." }
            val cleanPhone = shopPhone.trim().take(25)
            val cleanAddress = shopAddress.trim().take(250)

            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "shopName" to cleanName,
                        "shopPhone" to cleanPhone,
                        "shopAddress" to cleanAddress,
                        "preferredUnit" to preferredUnit.code,
                        "preferredLanguage" to preferredLanguage.code,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "shopName" to cleanName,
                        "shopPhone" to cleanPhone,
                        "shopAddress" to cleanAddress,
                        "preferredUnit" to preferredUnit.code,
                        "preferredLanguage" to preferredLanguage.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeCustomers(userId: String): Flow<List<Customer>> = flow {
        val uid = requireAuthenticatedUser(userId)
        val path = "users/$uid/customers"
        emitAll(
            db.collection("users").document(uid).collection("customers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Customer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCustomer(userId: String, customerId: String): Flow<Customer?> = flow {
        val uid = requireAuthenticatedUser(userId)
        val path = "users/$uid/customers/$customerId"
        emitAll(
            db.collection("users").document(uid).collection("customers").document(customerId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        Customer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeMeasurementHistory(userId: String, customerId: String): Flow<List<MeasurementHistory>> = flow {
        val uid = requireAuthenticatedUser(userId)
        val path = "users/$uid/customers/$customerId/history"
        emitAll(
            db.collection("users").document(uid)
                .collection("customers").document(customerId)
                .collection("history")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MeasurementHistory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createCustomer(
        userId: String,
        customerCode: String,
        fullName: String,
        mobileNumber: String,
        address: String,
        measurementDate: String,
        notes: String,
        measurement: ShalwarKameezMeasurement
    ): Result<String> {
        var docPath = "users/$userId/customers"
        return try {
            val uid = requireAuthenticatedUser(userId)
            val cleanCode = customerCode.trim().take(32)
            val cleanName = fullName.trim().take(100)
            val cleanMobile = mobileNumber.trim().take(25)
            val cleanAddress = address.trim().take(250)
            val cleanDate = measurementDate.trim().take(20)
            val cleanNotes = notes.trim().take(1000)

            require(cleanCode.isNotEmpty()) { "Customer ID is required." }
            require(cleanName.isNotEmpty()) { "Customer full name is required." }
            require(cleanMobile.length >= 4) { "Valid mobile number is required." }
            require(cleanDate.isNotEmpty()) { "Date of measurement is required." }

            val customersCol = db.collection("users").document(uid).collection("customers")

            // Prevent duplicate Customer IDs inside the tailor's shop
            val duplicateCheck = customersCol
                .whereEqualTo("customerCode", cleanCode)
                .limit(1)
                .get()
                .await()
            require(duplicateCheck.isEmpty) {
                "Customer ID \"$cleanCode\" already exists. Please use a unique ID."
            }

            val customerRef = customersCol.document()
            val historyRef = customerRef.collection("history").document()
            docPath = customerRef.path

            val measurementMap = measurement.toFirestoreMap()
            val customerPayload = mapOf(
                "userId" to uid,
                "customerCode" to cleanCode,
                "fullName" to cleanName,
                "mobileNumber" to cleanMobile,
                "address" to cleanAddress,
                "measurementDate" to cleanDate,
                "notes" to cleanNotes,
                "currentMeasurement" to measurementMap,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val historyPayload = mapOf(
                "userId" to uid,
                "customerId" to customerRef.id,
                "measurementDate" to cleanDate,
                "notes" to cleanNotes,
                "measurement" to measurementMap,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(customerRef, customerPayload)
            batch.set(historyRef, historyPayload)
            batch.commit().await()

            Result.success(customerRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCustomerInfo(
        userId: String,
        customerId: String,
        customerCode: String,
        fullName: String,
        mobileNumber: String,
        address: String,
        measurementDate: String,
        notes: String
    ): Result<Unit> {
        val docPath = "users/$userId/customers/$customerId"
        return try {
            val uid = requireAuthenticatedUser(userId)
            require(customerId.isNotBlank()) { "Invalid customer record." }

            val cleanCode = customerCode.trim().take(32)
            val cleanName = fullName.trim().take(100)
            val cleanMobile = mobileNumber.trim().take(25)
            val cleanAddress = address.trim().take(250)
            val cleanDate = measurementDate.trim().take(20)
            val cleanNotes = notes.trim().take(1000)

            require(cleanCode.isNotEmpty()) { "Customer ID is required." }
            require(cleanName.isNotEmpty()) { "Customer full name is required." }
            require(cleanMobile.length >= 4) { "Valid mobile number is required." }
            require(cleanDate.isNotEmpty()) { "Date of measurement is required." }

            val customersCol = db.collection("users").document(uid).collection("customers")
            val duplicateCheck = customersCol
                .whereEqualTo("customerCode", cleanCode)
                .get()
                .await()
            val hasConflict = duplicateCheck.documents.any { it.id != customerId }
            require(!hasConflict) {
                "Customer ID \"$cleanCode\" is already assigned to another customer."
            }

            customersCol.document(customerId).update(
                mapOf(
                    "customerCode" to cleanCode,
                    "fullName" to cleanName,
                    "mobileNumber" to cleanMobile,
                    "address" to cleanAddress,
                    "measurementDate" to cleanDate,
                    "notes" to cleanNotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCustomerMeasurements(
        userId: String,
        customerId: String,
        measurementDate: String,
        notes: String,
        measurement: ShalwarKameezMeasurement,
        recordHistory: Boolean = true
    ): Result<Unit> {
        val docPath = "users/$userId/customers/$customerId"
        return try {
            val uid = requireAuthenticatedUser(userId)
            require(customerId.isNotBlank()) { "Invalid customer record." }
            val cleanDate = measurementDate.trim().take(20)
            val cleanNotes = notes.trim().take(1000)
            require(cleanDate.isNotEmpty()) { "Date of measurement is required." }

            val customerRef = db.collection("users").document(uid)
                .collection("customers").document(customerId)
            val measurementMap = measurement.toFirestoreMap()

            val batch = db.batch()
            batch.update(
                customerRef,
                mapOf(
                    "measurementDate" to cleanDate,
                    "notes" to cleanNotes,
                    "currentMeasurement" to measurementMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (recordHistory) {
                val historyRef = customerRef.collection("history").document()
                batch.set(
                    historyRef,
                    mapOf(
                        "userId" to uid,
                        "customerId" to customerId,
                        "measurementDate" to cleanDate,
                        "notes" to cleanNotes,
                        "measurement" to measurementMap,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(userId: String, customerId: String): Result<Unit> {
        val docPath = "users/$userId/customers/$customerId"
        return try {
            val uid = requireAuthenticatedUser(userId)
            require(customerId.isNotBlank()) { "Invalid customer record." }

            val customerRef = db.collection("users").document(uid)
                .collection("customers").document(customerId)
            val historyDocs = customerRef.collection("history").get().await().documents

            historyDocs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            customerRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteHistoryEntry(
        userId: String,
        customerId: String,
        historyId: String
    ): Result<Unit> {
        val docPath = "users/$userId/customers/$customerId/history/$historyId"
        return try {
            val uid = requireAuthenticatedUser(userId)
            require(customerId.isNotBlank() && historyId.isNotBlank()) { "Invalid history entry." }
            db.collection("users").document(uid)
                .collection("customers").document(customerId)
                .collection("history").document(historyId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
