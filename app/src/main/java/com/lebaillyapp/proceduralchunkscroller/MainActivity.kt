package com.lebaillyapp.proceduralchunkscroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lebaillyapp.proceduralchunkscroller.rock.StaticTrenchView
import com.lebaillyapp.proceduralchunkscroller.test.ProceduralTestScreen
import com.lebaillyapp.proceduralchunkscroller.test.ScrollerTestScreen
import com.lebaillyapp.proceduralchunkscroller.ui.theme.ProceduralChunkScrollerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProceduralChunkScrollerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->


                //    ProceduralTestScreen(modifier =  Modifier.padding(innerPadding))

                //    ScrollerTestScreen(modifier =  Modifier.padding(innerPadding))

                    StaticTrenchView(modifier =  Modifier.padding(innerPadding))


                }
            }
        }
    }
}
