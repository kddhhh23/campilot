package com.example.campilot.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.campilot.domain.CampPlan
import kotlinx.coroutines.flow.first
private val Context.planData by preferencesDataStore("campilot_plan")
class PlanStore(private val context:Context) {
 suspend fun load():CampPlan { val p=context.planData.data.first(); fun n(key:String,default:Double)=p[doublePreferencesKey(key)]?:default
 return CampPlan(capacity=n("capacity",60.0),soc=n("soc",60.0),efficiency=n("efficiency",5.0),hours=n("hours",8.0),nextKm=n("next",25.0),temperature=n("temperature",22.0),minTemp=n("min",18.0),maxTemp=n("max",26.0),cooling=p[booleanPreferencesKey("cooling")]?:false) }
 suspend fun save(p:CampPlan){ context.planData.edit { v->mapOf("capacity" to p.capacity,"soc" to p.soc,"efficiency" to p.efficiency,"hours" to p.hours,"next" to p.nextKm,"temperature" to p.temperature,"min" to p.minTemp,"max" to p.maxTemp).forEach{(k,n)->v[doublePreferencesKey(k)]=n};v[booleanPreferencesKey("cooling")]=p.cooling } }
}
