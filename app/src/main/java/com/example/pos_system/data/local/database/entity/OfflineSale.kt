import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_sales")
data class OfflineSale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cartItems: String, // Store as JSON string
    val totalPrice: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false // Track if it's uploaded to Firebase
)