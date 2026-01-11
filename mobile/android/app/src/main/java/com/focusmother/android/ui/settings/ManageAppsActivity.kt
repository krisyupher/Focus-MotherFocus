package com.focusmother.android.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.domain.CategoryManager
import com.focusmother.android.ui.theme.FocusMotherFocusTheme

/**
 * ManageAppsActivity - Host activity for the ManageAppsScreen composable.
 */
class ManageAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = FocusMotherDatabase.getDatabase(this)
        val categoryManager = CategoryManager(database.appCategoryDao())
        
        setContent {
            FocusMotherFocusTheme {
                ManageAppsScreen(
                    categoryManager = categoryManager,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
