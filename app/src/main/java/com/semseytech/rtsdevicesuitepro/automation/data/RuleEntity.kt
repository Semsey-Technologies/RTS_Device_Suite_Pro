package com.semseytech.rtsdevicesuitepro.automation.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.semseytech.rtsdevicesuitepro.automation.models.Action
import com.semseytech.rtsdevicesuitepro.automation.models.Condition
import com.semseytech.rtsdevicesuitepro.automation.models.Trigger

@Entity(tableName = "rule_groups")
data class RuleGroup(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = ""
)

@Entity(
    tableName = "rules",
    foreignKeys = [
        ForeignKey(
            entity = RuleGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId")]
)
data class RuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val trigger: Trigger,
    val conditions: List<Condition>,
    val actions: List<Action>,
    val isEnabled: Boolean,
    val groupId: String? = null
)
