package tests

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.starmaen.accountingfrontend.MainScreen
import com.starmaen.accountingfrontend.RoleManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreenDisplaysCardsForAdminRole() {
        // إعداد دور المستخدم كمسؤول
        RoleManager.currentRole = RoleManager.Role.ADMIN
        composeTestRule.setContent { MainScreen() }
        // تحقق من ظهور بطاقة "تقارير" (مثال)
        composeTestRule.onNodeWithText("تقارير").assertIsDisplayed()
        // تحقق من ظهور بطاقة "المخزون"
        composeTestRule.onNodeWithText("المخزون").assertIsDisplayed()
    }

    @Test
    fun mainScreenDisplaysLimitedCardsForUserRole() {
        RoleManager.currentRole = RoleManager.Role.USER
        composeTestRule.setContent { MainScreen() }
        // بطاقة "تقارير" قد لا تظهر للمستخدم العادي
        composeTestRule.onNodeWithText("تقارير").assertDoesNotExist()
        // بطاقة "المخزون" يجب أن تظهر
        composeTestRule.onNodeWithText("المخزون").assertIsDisplayed()
    }
}
