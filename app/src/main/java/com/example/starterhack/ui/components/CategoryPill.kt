package com.example.starterhack.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starterhack.data.entities.Category
import com.example.starterhack.ui.theme.AutoAmber
import com.example.starterhack.ui.theme.AutoAmberBg
import com.example.starterhack.ui.theme.FinancialAmber
import com.example.starterhack.ui.theme.FinancialAmberBg
import com.example.starterhack.ui.theme.HomePink
import com.example.starterhack.ui.theme.HomePinkBg
import com.example.starterhack.ui.theme.IdGray
import com.example.starterhack.ui.theme.IdGrayBg
import com.example.starterhack.ui.theme.InsurancePurple
import com.example.starterhack.ui.theme.InsurancePurpleBg
import com.example.starterhack.ui.theme.MedicalBlue
import com.example.starterhack.ui.theme.MedicalBlueBg
import com.example.starterhack.ui.theme.NavyPrimary
import com.example.starterhack.ui.theme.TaxGreen
import com.example.starterhack.ui.theme.TaxGreenBg
import com.example.starterhack.ui.theme.TealAccent
import com.example.starterhack.ui.theme.TealWash

fun Category.pillColors(): Pair<Color, Color> = when (this) {
    Category.MEDICAL -> MedicalBlue to MedicalBlueBg
    Category.FINANCIAL -> FinancialAmber to FinancialAmberBg
    Category.TAX -> TaxGreen to TaxGreenBg
    Category.HOME -> HomePink to HomePinkBg
    Category.INSURANCE -> InsurancePurple to InsurancePurpleBg
    Category.ID_DOCUMENTS -> IdGray to IdGrayBg
    Category.IMMIGRATION -> TealAccent to TealWash
    Category.AUTO -> AutoAmber to AutoAmberBg
    Category.LEGAL -> NavyPrimary to MedicalBlueBg
    Category.OTHER -> IdGray to IdGrayBg
}

@Composable
fun CategoryPill(category: Category, modifier: Modifier = Modifier) {
    val (textColor, bgColor) = category.pillColors()
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bgColor,
        modifier = modifier,
    ) {
        Text(
            text = "${category.emoji} ${category.label}",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
