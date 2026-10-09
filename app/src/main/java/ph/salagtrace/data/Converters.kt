package ph.salagtrace.data

import androidx.room.TypeConverter
import ph.salagtrace.domain.*

class Converters {
    @TypeConverter fun sourceToText(value: Source) = value.name
    @TypeConverter fun textToSource(value: String) = Source.valueOf(value)
    @TypeConverter fun stateToText(value: AnalysisState) = value.name
    @TypeConverter fun textToState(value: String) = AnalysisState.valueOf(value)
    @TypeConverter fun verdictToText(value: Verdict) = value.name
    @TypeConverter fun textToVerdict(value: String) = Verdict.valueOf(value)
    @TypeConverter fun confidenceToText(value: Confidence) = value.name
    @TypeConverter fun textToConfidence(value: String) = Confidence.valueOf(value)
    @TypeConverter fun categoryToText(value: Category) = value.name
    @TypeConverter fun textToCategory(value: String) = Category.valueOf(value)
}
