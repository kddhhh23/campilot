package com.example.campilot.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Day = lightColorScheme(
    primary=Color(0xFF245C4D), onPrimary=Color.White,
    primaryContainer=Color(0xFFDCEBE3), onPrimaryContainer=Color(0xFF19352F),
    background=Color(0xFFEEF3F1), onBackground=Color(0xFF19352F),
    surface=Color.White, onSurface=Color(0xFF19352F),
    surfaceVariant=Color(0xFFE3EBE6), onSurfaceVariant=Color(0xFF526960),
    outline=Color(0xFF71857A), error=Color(0xFF9E352C)
)
@Composable fun CampilotTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=Day, typography=Typography(
        headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=38.sp,lineHeight=48.sp),
        headlineMedium=TextStyle(fontWeight=FontWeight.Bold,fontSize=30.sp,lineHeight=38.sp),
        titleLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=26.sp,lineHeight=34.sp),
        bodyLarge=TextStyle(fontSize=20.sp,lineHeight=30.sp),
        bodyMedium=TextStyle(fontSize=18.sp,lineHeight=27.sp),
        bodySmall=TextStyle(fontSize=16.sp,lineHeight=24.sp),
        labelLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=20.sp,lineHeight=28.sp)
    ),content=content)
}
@Composable fun Panel(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
    Surface(modifier=modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
    }
}
@Composable fun Page(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Text(title,style=MaterialTheme.typography.headlineLarge)
        Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
@Composable fun Metric(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(label,Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
    }
}
@Composable fun PrimaryAction(label:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp),
        shape=RoundedCornerShape(18.dp),contentPadding=PaddingValues(20.dp,16.dp)) { Text(label) }
}
@Composable fun Disclosure(title:String,content:@Composable ColumnScope.()->Unit) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        OutlinedButton(onClick={open=!open},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)
            .semantics { stateDescription=if(open) "펼쳐짐" else "접힘" },shape=RoundedCornerShape(16.dp)) {
            Text(title,Modifier.weight(1f))
            Text(if(open) "−" else "+",fontSize=26.sp)
        }
        if(open) Column(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
    }
}
@Composable fun StatusPill(text:String) {
    Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(50)) {
        Text(text,Modifier.padding(horizontal=16.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onPrimaryContainer,fontWeight=FontWeight.SemiBold)
    }
}
@Composable fun Notice(text:String,error:Boolean=false) {
    Surface(color=if(error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape=RoundedCornerShape(16.dp)) {
        Text(text,Modifier.fillMaxWidth().padding(20.dp),color=if(error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
/** Decorative landscape, deliberately fictional rather than an unverified campsite photo. */
@Composable fun CampLandscape(index:Int,modifier:Modifier=Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).clearAndSetSemantics {}) {
        val w=size.width; val h=size.height
        val sky=when(index%3){0->Color(0xFFD9E8D9);1->Color(0xFFD5E8EC);else->Color(0xFFE7E0D2)}
        val far=when(index%3){0->Color(0xFF94B4A1);1->Color(0xFF91B1BB);else->Color(0xFFB6AAA0)}
        val near=when(index%3){0->Color(0xFF4D7863);1->Color(0xFF517A7C);else->Color(0xFF7C8370)}
        drawRect(sky)
        drawCircle(Color(0xFFFFF3CD),h*.17f,Offset(w*.76f,h*.28f))
        val ridge=Path().apply {moveTo(0f,h*.65f);lineTo(w*.25f,h*.22f);lineTo(w*.5f,h*.62f);lineTo(w*.72f,h*.35f);lineTo(w,h*.64f);lineTo(w,h);lineTo(0f,h);close()}
        drawPath(ridge,far)
        val land=Path().apply{moveTo(0f,h*.72f);quadraticTo(w*.45f,h*.42f,w,h*.77f);lineTo(w,h);lineTo(0f,h);close()}
        drawPath(land,near)
        if(index%3==1) {
            drawOval(Color(0xFFA5CAD0),Offset(-w*.1f,h*.78f),Size(w*1.2f,h*.5f))
            drawLine(Color(0xFFD8EBE8),Offset(w*.1f,h*.9f),Offset(w*.43f,h*.9f),3f)
        } else {
            for(i in 0..4) {
                val x=w*(.08f+i*.16f); val y=h*(.71f+(i%2)*.1f)
                val tree=Path().apply{moveTo(x,y-h*.25f);lineTo(x-h*.11f,y);lineTo(x+h*.11f,y);close()}
                drawPath(tree,Color(0xFF294E40))
                drawLine(Color(0xFF294E40),Offset(x,y),Offset(x,y+h*.08f),4f)
            }
        }
        // Quiet camping motif: a tent on the right bank.
        val tent=Path().apply {moveTo(w*.65f,h*.86f);lineTo(w*.76f,h*.55f);lineTo(w*.89f,h*.86f);close()}
        drawPath(tent,Color(0xFFECC88F))
        drawLine(Color(0xFF805F3C),Offset(w*.76f,h*.55f),Offset(w*.79f,h*.86f),3f)
        drawLine(Color(0xFF805F3C),Offset(w*.65f,h*.86f),Offset(w*.89f,h*.86f),3f)
    }
}
@Composable fun BatteryMeter(percent:Double?,label:String) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(percent?.let{"${kotlin.math.round(it).toInt()}%"} ?: "확인 중",fontSize=52.sp,fontWeight=FontWeight.Bold)
        LinearProgressIndicator(progress={((percent ?: 0.0)/100).toFloat().coerceIn(0f,1f)},
            modifier=Modifier.fillMaxWidth().height(12.dp),color=MaterialTheme.colorScheme.primary,
            trackColor=MaterialTheme.colorScheme.surfaceVariant)
    }
}
