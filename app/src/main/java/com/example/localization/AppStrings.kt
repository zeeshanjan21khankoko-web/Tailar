package com.example.localization

import com.example.model.LanguageMode
import com.example.model.MeasurementUnit
import com.example.model.ShalwarStyleOption

data class BiText(val en: String, val ur: String) {
    fun primary(mode: LanguageMode): String = when (mode) {
        LanguageMode.EN -> en
        LanguageMode.UR -> ur
        LanguageMode.BOTH -> en
    }

    fun secondary(mode: LanguageMode): String? = when (mode) {
        LanguageMode.BOTH -> ur
        else -> null
    }

    fun inline(mode: LanguageMode): String = when (mode) {
        LanguageMode.EN -> en
        LanguageMode.UR -> ur
        LanguageMode.BOTH -> "$en • $ur"
    }
}

object AppStrings {
    val appTitle = BiText("TailorBook", "ٹیلر بک")
    val appSubtitle = BiText("Shalwar Kameez Measurement Studio", "شلوار قمیض پیمائش رجسٹر")

    // Navigation
    val navDashboard = BiText("Dashboard", "ڈیش بورڈ")
    val navCustomers = BiText("Customers", "گاہک")
    val navSettings = BiText("Shop & Settings", "دکان و ترتیبات")

    // Auth
    val authHeadline = BiText("Your Digital Tailoring Register", "آپ کا ڈیجیٹل درزی رجسٹر")
    val authBody = BiText(
        "Save Shalwar Kameez measurements permanently in the cloud, track fitting history, print receipts, and share on WhatsApp.",
        "شلوار قمیض کی پیمائش محفوظ کریں، پرانا ریکارڈ دیکھیں، رسید پرنٹ کریں اور واٹس ایپ پر شیئر کریں۔"
    )
    val signInWithGoogle = BiText("Sign in with Google", "گوگل کے ساتھ سائن ان کریں")
    val featureCloud = BiText("Permanent Cloud Backup", "محفوظ کلاؤڈ بیک اپ")
    val featurePdf = BiText("PDF Receipts & Printing", "پی ڈی ایف رسید اور پرنٹ")
    val featureWhatsApp = BiText("Instant WhatsApp Share", "فوری واٹس ایپ شیئرنگ")

    // Dashboard
    val totalCustomers = BiText("Total Customers", "کل گاہک")
    val recentCustomers = BiText("Recently Added", "حالیہ گاہک")
    val addNewCustomer = BiText("Add New Customer", "نیا گاہک شامل کریں")
    val searchPlaceholder = BiText("Search by name, mobile, or ID...", "نام، موبائل یا آئی ڈی سے تلاش کریں...")
    val viewAll = BiText("View All", "سب دیکھیں")
    val emptyCustomersTitle = BiText("No Customers Yet", "ابھی کوئی گاہک موجود نہیں")
    val emptyCustomersBody = BiText(
        "Tap Add New Customer to record your first customer profile and Shalwar Kameez measurements.",
        "پہلا گاہک اور شلوار قمیض کی پیمائش درج کرنے کے لیے نیا گاہک شامل کریں دبائیں۔"
    )
    val noSearchMatches = BiText("No matching customers found", "کوئی گاہک نہیں ملا")

    // Customer Form
    val newCustomerTitle = BiText("New Customer", "نیا گاہک")
    val editCustomerTitle = BiText("Edit Customer Info", "گاہک کی معلومات میں ترمیم")
    val editMeasurementsTitle = BiText("Update Measurements", "پیمائش اپ ڈیٹ کریں")
    val stepCustomerInfo = BiText("Customer Info", "گاہک کی معلومات")
    val stepMeasurements = BiText("Measurements", "پیمائش")
    val nextToMeasurements = BiText("Next: Measurements", "اگلا: پیمائش")
    val saveCustomerAndMeasurements = BiText("Save Measurement Profile", "پیمائش محفوظ کریں")
    val updateCustomerInfo = BiText("Save Customer Info", "معلومات محفوظ کریں")
    val saveNewMeasurementVersion = BiText("Save Updated Measurements", "نئی پیمائش محفوظ کریں")

    // Customer Fields
    val fullName = BiText("Customer Full Name", "گاہک کا پورا نام")
    val mobileNumber = BiText("Mobile Number", "موبائل نمبر")
    val address = BiText("Address", "پتہ")
    val customerId = BiText("Customer ID", "کسٹمر آئی ڈی")
    val measurementDate = BiText("Date of Measurement", "پیمائش کی تاریخ")
    val notes = BiText("Additional Notes", "اضافی نوٹس")

    // Measurement Sections
    val unitLabel = BiText("Measurement Unit", "پیمائش کی اکائی")
    val inches = BiText("Inches (in)", "انچ")
    val centimeters = BiText("Centimeters (cm)", "سینٹی میٹر")
    val convertValuesPrompt = BiText("Convert existing values when switching unit", "اکائی بدلنے پر نمبر تبدیل کریں")
    val kameezSection = BiText("Kameez / Shirt Measurements", "قمیض کی پیمائش")
    val shalwarSection = BiText("Shalwar Measurements", "شلوار کی پیمائش")

    // Kameez Fields (12)
    val kameezLength = BiText("Length", "لمبائی")
    val shoulder = BiText("Shoulder", "تیرا / کندھا")
    val chest = BiText("Chest", "چھاتی")
    val kameezWaist = BiText("Waist", "کمر")
    val kameezHip = BiText("Hip", "گھیرا / ہپ")
    val sleeveLength = BiText("Sleeve Length", "آستین")
    val armhole = BiText("Armhole", "مونڈھا")
    val neck = BiText("Neck", "گلا")
    val cuff = BiText("Cuff", "کف")
    val frontLength = BiText("Front Length", "اگلا دامن")
    val backLength = BiText("Back Length", "پچھلا دامن")
    val collar = BiText("Collar", "کالر")

    // Shalwar Fields (7)
    val shalwarLength = BiText("Shalwar Length", "شلوار لمبائی")
    val shalwarWaist = BiText("Waist", "کمر / نیفہ")
    val shalwarHip = BiText("Hip", "آسن / ہپ")
    val thigh = BiText("Thigh", "ران")
    val knee = BiText("Knee", "گھٹنا")
    val bottomPaicha = BiText("Bottom / Paicha", "پائنچہ")
    val shalwarStyle = BiText("Shalwar Style", "شلوار کا انداز")

    // Detail & Actions
    val printReceipt = BiText("Print Receipt", "رسید پرنٹ کریں")
    val downloadPdf = BiText("Download PDF", "پی ڈی ایف ڈاؤن لوڈ")
    val shareWhatsApp = BiText("WhatsApp", "واٹس ایپ شیئر")
    val shareWhatsAppText = BiText("Send Text on WhatsApp", "واٹس ایپ ٹیکسٹ بھیجیں")
    val shareWhatsAppPdf = BiText("Share PDF Receipt", "پی ڈی ایف رسید شیئر کریں")
    val editInfo = BiText("Edit Info", "معلومات بدلیں")
    val updateMeasurements = BiText("Update Measurements", "پیمائش بدلیں")
    val deleteCustomer = BiText("Delete Customer", "ریکارڈ حذف کریں")
    val deleteConfirmTitle = BiText("Delete Customer Record?", "کیا آپ ریکارڈ حذف کرنا چاہتے ہیں؟")
    val deleteConfirmBody = BiText(
        "This will permanently delete this customer profile and all measurement history. This cannot be undone.",
        "یہ گاہک کا پروفائل اور تمام پرانی پیمائشیں مستقل طور پر حذف کر دے گا۔"
    )
    val cancel = BiText("Cancel", "منسوخ کریں")
    val confirmDelete = BiText("Delete Permanently", "حذف کریں")
    val measurementHistory = BiText("Measurement History", "پیمائش کی تاریخچہ")
    val noHistoryYet = BiText("Initial measurement recorded", "پہلی پیمائش درج ہے")
    val restoreVersion = BiText("Use These Measurements", "یہ پیمائش لاگو کریں")

    // Settings
    val shopName = BiText("Tailor Shop Name", "درزی کی دکان کا نام")
    val shopPhone = BiText("Shop Phone", "دکان کا فون نمبر")
    val shopAddress = BiText("Shop Address", "دکان کا پتہ")
    val saveSettings = BiText("Save Shop Profile", "دکان کی معلومات محفوظ کریں")
    val languageLabel = BiText("Display Language", "زبان منتخب کریں")
    val signOut = BiText("Sign Out", "سائن آؤٹ")

    fun unitShort(unitCode: String, mode: LanguageMode): String {
        return when (MeasurementUnit.fromCode(unitCode)) {
            MeasurementUnit.INCHES -> when (mode) {
                LanguageMode.UR -> "انچ"
                else -> "in"
            }
            MeasurementUnit.CENTIMETERS -> when (mode) {
                LanguageMode.UR -> "سینٹی میٹر"
                else -> "cm"
            }
        }
    }

    fun formatShalwarStyle(rawStyle: String, mode: LanguageMode): String {
        val option = ShalwarStyleOption.entries.firstOrNull {
            it.en.equals(rawStyle, ignoreCase = true) || it.ur == rawStyle
        } ?: return rawStyle
        return when (mode) {
            LanguageMode.EN -> option.en
            LanguageMode.UR -> option.ur
            LanguageMode.BOTH -> "${option.en} • ${option.ur}"
        }
    }
}
