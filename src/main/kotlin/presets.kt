// This file adds presets. PridePresets are based on hyfetch, and were last updated 2024-06-12
// https://github.com/hykilpikonna/hyfetch/blob/7534371b05ee877cd3c4c3733b13d7c41d09c3e/hyfetch/presets.py


package chattore

fun weighted(vararg colors: Pair<String, Int>): Array<String> =
        colors.flatMap { (value, weight) -> List(weight) { value } }.toTypedArray()

val prideColors = mapOf(
    "rainbow" to arrayOf(
        "#E50000",
        "#FF8D00",
        "#FFEE00",
        "#028121",
        "#004CFF",
        "#770088"
    ),

    "transgender" to arrayOf(
        "#55CDFD",
        "#F6AAB7",
        "#FFFFFF",
        "#F6AAB7",
        "#55CDFD"
    ),

    "nonbinary" to arrayOf(
        "#FCF431",
        "#FCFCFC",
        "#9D59D2",
        "#282828"
    ),

    "agender" to arrayOf(
        "#000000",
        "#BABABA",
        "#FFFFFF",
        "#BAF484",
        "#FFFFFF",
        "#BABABA",
        "#000000"
    ),

    "queer" to arrayOf(
        "#B57FDD",
        "#FFFFFF",
        "#49821E"
    ),

    "genderfluid" to arrayOf(
        "#FE76A2",
        "#FFFFFF",
        "#BF12D7",
        "#000000",
        "#303CBE"
    ),

    "bisexual" to arrayOf(
        "#D60270",
        "#9B4F96",
        "#0038A8"
    ),

    "pansexual" to arrayOf(
        "#FF1C8D",
        "#FFD700",
        "#1AB3FF"
    ),

    "polysexual" to arrayOf(
        "#F714BA",
        "#01D66A",
        "#1594F6",
    ),

    "omnisexual" to arrayOf(
        "#FE9ACE",
        "#FF53BF",
        "#200044",
        "#6760FE",
        "#8EA6FF",
    ),

    "omniromantic" to arrayOf(
        "#FEC8E4",
        "#FDA1DB",
        "#89739A",
        "#ABA7FE",
        "#BFCEFF",
    ),

    "gay-men" to arrayOf(
        "#078D70",
        "#98E8C1",
        "#FFFFFF",
        "#7BADE2",
        "#3D1A78"
    ),

    "lesbian" to arrayOf(
        "#D62800",
        "#FF9B56",
        "#FFFFFF",
        "#D462A6",
        "#A40062"
    ),

    "abrosexual" to arrayOf(
        "#46D294",
        "#A3E9CA",
        "#FFFFFF",
        "#F78BB3",
        "#EE1766",
    ),

    "asexual" to arrayOf(
        "#000000",
        "#A4A4A4",
        "#FFFFFF",
        "#810081"
    ),

    "aromantic" to arrayOf(
        "#3BA740",
        "#A8D47A",
        "#FFFFFF",
        "#ABABAB",
        "#000000"
    ),

    "aroace1" to arrayOf(
        "#E28C00",
        "#ECCD00",
        "#FFFFFF",
        "#62AEDC",
        "#203856"
    ),

    "aroace2" to arrayOf(
        "#000000",
        "#810081",
        "#A4A4A4",
        "#FFFFFF",
        "#A8D47A",
        "#3BA740"
    ),

    "aroace3" to arrayOf(
        "#3BA740",
        "#A8D47A",
        "#FFFFFF",
        "#ABABAB",
        "#000000",
        "#A4A4A4",
        "#FFFFFF",
        "#810081"
    ),

    "autosexual" to arrayOf(
        "#99D9EA",
        "#7F7F7F"
    ),

    "intergender" to arrayOf(
        "#900DC2",
        "#900DC2",
        "#FFE54F",
        "#900DC2",
        "#900DC2",
    ),

    "greygender" to arrayOf(
        "#B3B3B3",
        "#B3B3B3",
        "#FFFFFF",
        "#062383",
        "#062383",
        "#FFFFFF",
        "#535353",
        "#535353",
    ),

    "akiosexual" to arrayOf(
        "#F9485E",
        "#FEA06A",
        "#FEF44C",
        "#FFFFFF",
        "#000000",
    ),

    "bigender" to arrayOf(
        "#C479A2",
        "#EDA5CD",
        "#D6C7E8",
        "#FFFFFF",
        "#D6C7E8",
        "#9AC7E8",
        "#6D82D1",
    ),

    "demigender" to arrayOf(
        "#7F7F7F",
        "#C4C4C4",
        "#FBFF75",
        "#FFFFFF",
        "#FBFF75",
        "#C4C4C4",
        "#7F7F7F",
    ),

    "demiboy" to arrayOf(
        "#7F7F7F",
        "#C4C4C4",
        "#9DD7EA",
        "#FFFFFF",
        "#9DD7EA",
        "#C4C4C4",
        "#7F7F7F",
    ),

    "demigirl" to arrayOf(
        "#7F7F7F",
        "#C4C4C4",
        "#FDADC8",
        "#FFFFFF",
        "#FDADC8",
        "#C4C4C4",
        "#7F7F7F",
    ),

    "transmasculine" to arrayOf(
        "#FF8ABD",
        "#CDF5FE",
        "#9AEBFF",
        "#74DFFF",
        "#9AEBFF",
        "#CDF5FE",
        "#FF8ABD",
    ),

    "transfeminine" to arrayOf(
        "#73DEFF",
        "#FFE2EE",
        "#FFB5D6",
        "#FF8DC0",
        "#FFB5D6",
        "#FFE2EE",
        "#73DEFF",
    ),

    "genderfaun" to arrayOf(
        "#FCD689",
        "#FFF09B",
        "#FAF9CD",
        "#FFFFFF",
        "#8EDED9",
        "#8CACDE",
        "#9782EC",
    ),

    "demifaun" to arrayOf(
        "#7F7F7F",
        "#7F7F7F",
        "#C6C6C6",
        "#C6C6C6",
        "#FCC688",
        "#FFF19C",
        "#FFFFFF",
        "#8DE0D5",
        "#9682EC",
        "#C6C6C6",
        "#C6C6C6",
        "#7F7F7F",
        "#7F7F7F",
    ),

    "genderfae" to arrayOf(
        "#97C3A5",
        "#C3DEAE",
        "#F9FACD",
        "#FFFFFF",
        "#FCA2C4",
        "#DB8AE4",
        "#A97EDD",
    ),

    "demifae" to arrayOf(
        "#7F7F7F",
        "#7F7F7F",
        "#C5C5C5",
        "#C5C5C5",
        "#97C3A4",
        "#C4DEAE",
        "#FFFFFF",
        "#FCA2C5",
        "#AB7EDF",
        "#C5C5C5",
        "#C5C5C5",
        "#7F7F7F",
        "#7F7F7F",
    ),

    "neutrois" to arrayOf(
        "#FFFFFF",
        "#1F9F00",
        "#000000"
    ),

    "biromantic1" to arrayOf(
        "#8869A5",
        "#D8A7D8",
        "#FFFFFF",
        "#FDB18D",
        "#151638",
    ),

    "biromantic2" to arrayOf(
        "#740194",
        "#AEB1AA",
        "#FFFFFF",
        "#AEB1AA",
        "#740194",
    ),

    "autoromantic" to arrayOf(
        "#99D9EA",
        "#99D9EA",
        "#3DA542",
        "#7F7F7F",
        "#7F7F7F",
    ),

    "boyflux2" to weighted(
        Pair("#E48AE4",1),
        Pair("#9A81B4",1),
        Pair("#55BFAB",1),
        Pair("#FFFFFF",1),
        Pair("#A8A8A8",1),
        Pair("#81D5EF",5),
        Pair("#69ABE5",5),
        Pair("#5276D4",5),
    ),

    "girlflux" to arrayOf(
        "#F9E6D7",
        "#F2526C",
        "#BF0311",
        "#E9C587",
        "#BF0311",
        "#F2526C",
        "#F9E6D7",
    ),

    "genderflux" to arrayOf(
        "#F47694",
        "#F2A2B9",
        "#CECECE",
        "#7CE0F7",
        "#3ECDF9",
        "#FFF48D",
    ),

    "finsexual" to arrayOf(
        "#B18EDF",
        "#D7B1E2",
        "#F7CDE9",
        "#F39FCE",
        "#EA7BB3",
    ),

    "unlabeled1" to arrayOf(
        "#EAF8E4",
        "#FDFDFB",
        "#E1EFF7",
        "#F4E2C4"
    ),

    "unlabeled2" to arrayOf(
        "#250548",
        "#FFFFFF",
        "#F7DCDA",
        "#EC9BEE",
        "#9541FA",
        "#7D2557"
    ),

    "pangender" to arrayOf(
        "#FFF798",
        "#FEDDCD",
        "#FFEBFB",
        "#FFFFFF",
        "#FFEBFB",
        "#FEDDCD",
        "#FFF798",
    ),

    "gendernonconforming1" to weighted(
        Pair("#50284D",4),
        Pair("#96467B",1),
        Pair("#5C96F7",1),
        Pair("#FFE6F7",1),
        Pair("#5C96F7",1),
        Pair("#96467B",1),
        Pair("#50284D",4),
    ),

    "gendernonconforming2" to arrayOf(
        "#50284D",
        "#96467B",
        "#5C96F7",
        "#FFE6F7",
        "#5C96F7",
        "#96467B",
        "#50284D"
    ),

    "femboy" to arrayOf(
        "#D260A5",
        "#E4AFCD",
        "#FEFEFE",
        "#57CEF8",
        "#FEFEFE",
        "#E4AFCD",
        "#D260A5"
    ),

    "tomboy" to arrayOf(
        "#2F3FB9",
        "#613A03",
        "#FEFEFE",
        "#F1A9B7",
        "#FEFEFE",
        "#613A03",
        "#2F3FB9"
    ),

    "gynesexual" to arrayOf(
        "#F4A9B7",
        "#903F2B",
        "#5B953B",
    ),

    "androsexual" to arrayOf(
        "#01CCFF",
        "#603524",
        "#B799DE",
    ),

    "gendervoid" to arrayOf(
        "#081149",
        "#4B484B",
        "#000000",
        "#4B484B",
        "#081149"
    ),

    "voidgirl" to arrayOf(
        "#180827",
        "#7A5A8B",
        "#E09BED",
        "#7A5A8B",
        "#180827"
    ),

    "voidboy" to arrayOf(
        "#0B130C",
        "#547655",
        "#66B969",
        "#547655",
        "#0B130C"
    ),

    "nonhuman-unity" to arrayOf(
        "#177B49",
        "#FFFFFF",
        "#593C90"
    ),

    "plural" to arrayOf(
        "#2D0625",
        "#543475",
        "#7675C3",
        "#89C7B0",
        "#F3EDBD",
    ),

    "fraysexual" to arrayOf(
        "#226CB5",
        "#94E7DD",
        "#FFFFFF",
        "#636363",
    ),

    "beiyang" to arrayOf(
        "#DF1B12",
        "#FFC600",
        "#01639D",
        "#FFFFFF",
        "#000000",
    ),

    "burger" to arrayOf(
        "#F3A26A",
        "#498701",
        "#FD1C13",
        "#7D3829",
        "#F3A26A",
    ),

    "baker" to arrayOf(
        "#F23D9E",
        "#F80A24",
        "#F78022",
        "#F9E81F",
        "#1E972E",
        "#1B86BC",
        "#243897",
        "#6F0A82",
    ),
)

val birdflopColors = mapOf(
    "birdflop" to arrayOf(
        "#084CFB",
        "#ADF3FD"
    ),
    "SimplyMC" to arrayOf (
        "#084CFB",
        "#ADF3FD"
    ),
    "Rainbow" to arrayOf (
        "#FF0000",
        "#FF7F00",
        "#FFFF00",
        "#00FF00",
        "#0000FF",
        "#EAE4AA"
    ),
    "Skyline" to arrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Mango" to arrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Vice City" to arrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Dawn" to arrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Rose" to arrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Firewatch" to arrayOf (
        "#1488CC",
        "#2B32B2"
    ),        
)

val playerColors = mapOf(
    "Waffle" to arrayOf (
        "#1488CC",
        "#2B32B2"
    ),
)

val pridePresets = prideColors.toGradientMinimessage()
val birdflopPresets = birdflopColors.toGradientMinimessage()
val playerPresets = playerColors.toGradientMinimessage()
