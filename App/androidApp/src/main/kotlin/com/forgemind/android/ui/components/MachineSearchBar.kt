package com.forgemind.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.forgemind.android.ui.theme.PrimaryBlue
import com.forgemind.android.ui.theme.SurfaceDark

@Composable
fun MachineSearchBar() {

    val searchText = remember {
        mutableStateOf("")
    }

    OutlinedTextField(
        value = searchText.value,
        onValueChange = {
            searchText.value = it
        },

        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        placeholder = {
            Text("Search Machine ID...")
        },

        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },

        singleLine = true,

        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = SurfaceDark
        ),

        shape = MaterialTheme.shapes.large
    )
}