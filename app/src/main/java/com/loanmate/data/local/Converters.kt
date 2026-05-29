package com.loanmate.data.local

import androidx.room.TypeConverter
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit

class Converters {
    @TypeConverter fun fromLoanType(v: LoanType) = v.name
    @TypeConverter fun toLoanType(v: String) = LoanType.valueOf(v)

    @TypeConverter fun fromInterestType(v: InterestType) = v.name
    @TypeConverter fun toInterestType(v: String) = InterestType.valueOf(v)

    @TypeConverter fun fromTenureUnit(v: TenureUnit) = v.name
    @TypeConverter fun toTenureUnit(v: String) = TenureUnit.valueOf(v)

    @TypeConverter fun fromLoanStatus(v: LoanStatus) = v.name
    @TypeConverter fun toLoanStatus(v: String) = LoanStatus.valueOf(v)

    @TypeConverter fun fromDocumentType(v: DocumentType) = v.name
    @TypeConverter fun toDocumentType(v: String) = DocumentType.valueOf(v)

    @TypeConverter fun fromAchievementType(v: AchievementType) = v.name
    @TypeConverter fun toAchievementType(v: String) = AchievementType.valueOf(v)
}
