package com.example.starterhack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starterhack.benchmark.BenchmarkReceiver
import com.example.starterhack.navigation.ShieldTextNavGraph
import com.example.starterhack.ui.RedactionViewModel
import com.example.starterhack.ui.theme.ShieldTextTheme
import com.example.starterhack.ui.viewmodel.DocumentViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShieldTextTheme {
                val vm: RedactionViewModel = viewModel(
                    factory = RedactionViewModel.factory()
                )
                val docViewModel: DocumentViewModel = viewModel(
                    factory = DocumentViewModel.Factory
                )

                BenchmarkReceiver.onBenchmarkRequested = { category, count, backend, type, difficulty ->
                    Log.i("RedactoBenchmark", "Callback: type=$type, count=$count, backend=$backend, difficulty=$difficulty")
                    vm.runBenchmark(category, count, backend, type, difficulty)
                }

                ShieldTextNavGraph(
                    redactionViewModel = vm,
                    docViewModel = docViewModel,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BenchmarkReceiver.onBenchmarkRequested = null
    }
}
