package com.example.bananaq

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

private val agreementTagalog = listOf(
    "User Agreement" to "Kasunduan ng Gumagamit",
    "Effective Date:" to "Petsa ng Pagkakabisa: Hunyo 2026",
    "Welcome to BananaQ." to "Maligayang pagdating sa BananaQ. Sa pag-download, pag-install, pag-access, o paggamit ng BananaQ mobile application (\"App\"), sumasang-ayon kang sundin at mapasailalim sa Kasunduang ito ng Gumagamit.",
    "If you do not agree" to "Kung hindi ka sumasang-ayon sa alinmang bahagi ng Kasunduang ito, hindi mo dapat gamitin ang application.",
    "1. Purpose of the Application" to "1. Layunin ng Application",
    "BananaQ is an educational" to "Ang BananaQ ay isang application para sa edukasyon at suportang pang-agrikultura na tumutulong sa pagtukoy at pag-uuri ng mga sakit sa dahon ng saging gamit ang image analysis at Convolutional Neural Network (CNN).\n\nKasalukuyang natutukoy ng application ang:\n•  Black Sigatoka\n•  Panama Disease\n•  Cordana Leaf Spot",
    "2. Eligibility" to "2. Kwalipikasyon",
    "You may use BananaQ if you:" to "Maaari mong gamitin ang BananaQ kung ikaw ay:\n•  Hindi bababa sa labingwalong (18) taong gulang, o\n•  May pahintulot at gabay ng magulang, guardian, o awtorisadong organisasyon.",
    "3. User Responsibilities" to "3. Mga Pananagutan ng Gumagamit",
    "By using BananaQ, you agree to:" to "Sa paggamit ng BananaQ, sumasang-ayon kang:\n•  Magbigay ng tama at legal na impormasyon kapag kinakailangan.\n•  Mag-upload lamang ng mga larawang pag-aari mo o may pahintulot kang gamitin.\n•  Gamitin ang application para lamang sa legal at lehitimong layuning pang-agrikultura.\n•  Huwag mag-upload ng mapaminsala, ilegal, nakasasakit, o malicious na content.",
    "4. Accuracy of Results" to "4. Katumpakan ng mga Resulta",
    "BananaQ uses Artificial Intelligence" to "Gumagamit ang BananaQ ng Artificial Intelligence at machine learning upang gumawa ng prediksiyon.\n\nKinikilala ng mga gumagamit na:\n•  Ang resulta ay pagtataya batay sa image analysis.\n•  Hindi nito dapat palitan ang konsultasyon sa propesyonal sa agrikultura.\n•  Hindi pananagutan ng mga developer ang pagkawala ng ani, salapi, o pinsalang dulot ng pag-asa lamang sa prediksiyon ng application.",
    "5. Availability of Service" to "5. Pagiging Available ng Serbisyo",
    "The developers may update" to "Maaaring i-update, pahusayin, pansamantalang ihinto, o alisin ng mga developer ang ilang bahagi ng application nang walang paunang abiso para sa maintenance, pananaliksik, o development.",
    "6. Termination" to "6. Pagwawakas",
    "The developers reserve the right" to "May karapatan ang mga developer na limitahan o wakasan ang access ng mga gumagamit na:\n•  Lumalabag sa Kasunduang ito,\n•  Maling gumagamit ng application,\n•  Nagtatangkang magkaroon ng hindi awtorisadong access, o\n•  Gumagambala sa operasyon ng application.",
    "Terms of Use" to "Mga Tuntunin ng Paggamit",
    "1. Acceptance of Terms" to "1. Pagtanggap sa mga Tuntunin",
    "By using BananaQ, you agree to these" to "Sa paggamit ng BananaQ, sumasang-ayon ka sa mga Tuntunin ng Paggamit at sa mga susunod na pagbabago nito.",
    "2. Permitted Use" to "2. Pinahihintulutang Paggamit",
    "You may use BananaQ for:" to "Maaari mong gamitin ang BananaQ para sa:\n•  Pagtukoy ng sakit sa dahon ng saging,\n•  Pag-aaral tungkol sa agrikultura,\n•  Tulong sa pagsubaybay ng pananim,\n•  Akademiko at pananaliksik.",
    "3. Prohibited Activities" to "3. Mga Ipinagbabawal na Gawain",
    "Users shall not:" to "Hindi dapat:\n•  I-reverse engineer o tangkaing kunin ang source code ng application.\n•  Baguhin o ipamahagi ang application nang walang pahintulot.\n•  Mag-upload ng virus, malware, o mapaminsalang file.\n•  Gamitin ang application sa panloloko o ilegal na gawain.\n•  Manipulahin o pagsamantalahan ang AI prediction system.",
    "4. Intellectual Property" to "4. Intellectual Property",
    "All rights, including" to "Ang lahat ng karapatan, kabilang ang:\n•  Software,\n•  Source code,\n•  User interface,\n•  AI models,\n•  Graphics,\n•  Dokumentasyon,\n•  Mga logo, at\n•  Iba pang content,\nay nananatiling intellectual property ng BananaQ research and development team maliban kung iba ang nakasaad.\n\nHindi maaaring kopyahin o muling ipamahagi ang mga ito nang walang nakasulat na pahintulot.",
    "5. Third-Party Technologies" to "5. Mga Teknolohiya ng Third Party",
    "BananaQ may use third-party" to "Maaaring gumamit ang BananaQ ng third-party frameworks, libraries, o machine learning models. Ang mga ito ay napapailalim sa kani-kanilang lisensiya.",
    "6. Disclaimer" to "6. Disclaimer",
    "The application is provided" to "Ibinibigay ang application sa kalagayang \"as is\" at \"as available\".\n\nWalang garantiya ang mga developer tungkol sa:\n•  Tuloy-tuloy na availability,\n•  Ganap na katumpakan,\n•  Operasyong walang error,\n•  Pagiging angkop sa lahat ng kondisyon ng pagsasaka.",
    "7. Limitation of Liability" to "7. Limitasyon ng Pananagutan",
    "To the maximum extent" to "Hangga't pinahihintulutan ng batas, hindi mananagot ang mga developer para sa:\n•  Pinsala sa pananim,\n•  Pagkalugi sa pananalapi,\n•  Pagkaantala ng negosyo,\n•  Pagkawala ng data,\n•  Hindi direkta o kasunod na pinsala,\nna nagmula sa paggamit o kawalan ng kakayahang gamitin ang BananaQ.",
    "8. Changes to the Terms" to "8. Mga Pagbabago sa mga Tuntunin",
    "The developers may revise these Terms" to "Maaaring baguhin ng mga developer ang mga Tuntunin ng Paggamit. Ang patuloy na paggamit sa application ay nangangahulugang tinatanggap mo ang mga binagong tuntunin.",
    "Privacy Policy" to "Patakaran sa Privacy",
    "BananaQ respects the privacy" to "Iginagalang ng BananaQ ang privacy ng mga gumagamit at nakatuon itong protektahan ang personal na impormasyon.",
    "1. Information We Collect" to "1. Impormasyong Kinokolekta Namin",
    "The application may collect" to "Maaaring kolektahin ng application ang sumusunod:\n\na. Impormasyon ng Gumagamit\n•  Pangalan\n•  Email address\n•  Feedback at sagot sa survey\n\nb. Mga In-upload na Larawan\n•  Mga larawan ng dahon ng saging na isinumite para sa pagsusuri.\n\nc. Impormasyon ng Device\n•  Bersiyon ng Android\n•  Modelo ng device\n•  Bersiyon ng application\n•  Diagnostic logs para sa troubleshooting\n\nd. Usage Data\n•  Dalas ng paggamit\n•  Kasaysayan ng pagtukoy ng sakit\n•  Estadistika ng paggamit ng feature",
    "2. How We Use the Information" to "2. Paano Namin Ginagamit ang Impormasyon",
    "Collected information may be used" to "Maaaring gamitin ang nakolektang impormasyon upang:\n•  Suriin ang larawan ng dahon,\n•  Pahusayin ang AI model,\n•  Pahusayin ang functionality ng application,\n•  Gumawa ng anonymous na estadistika para sa pananaliksik,\n•  Magbigay ng rekomendasyon sa paggamot at pag-iwas,\n•  Lutasin ang mga teknikal na problema.",
    "3. Data Storage and Security" to "3. Pag-iimbak at Seguridad ng Data",
    "Reasonable technical and administrative" to "Nagpapatupad ng makatwirang teknikal at administratibong proteksiyon laban sa hindi awtorisadong access, pagbubunyag, o maling paggamit.\n\nGayunman, walang electronic storage o internet transmission na ganap na ligtas at hindi magagarantiya ang lubos na seguridad.",
    "4. Data Sharing" to "4. Pagbabahagi ng Data",
    "BananaQ does not sell" to "Hindi ibinebenta o pinauupahan ng BananaQ ang personal na impormasyon.\n\nMaaari lamang itong ibahagi:\n•  Kapag hinihingi ng batas,\n•  Para sa aprubadong akademiko o pananaliksik na naka-anonymize,\n•  Sa awtorisadong service providers na tumutulong sa operasyon.",
    "5. Research Use" to "5. Paggamit sa Pananaliksik",
    "As BananaQ is developed" to "Dahil binuo ang BananaQ bilang academic capstone project, maaaring gamitin ang anonymous at pinagsama-samang data para sa:\n•  Pagsusuri ng system,\n•  Performance analysis,\n•  Akademikong publikasyon,\n•  Hinaharap na research and development.\n\nWalang personally identifiable information na ibubunyag nang walang pahintulot maliban kung hinihingi ng batas.",
    "6. Data Retention" to "6. Pananatili ng Data",
    "User data will be retained" to "Pananatilihin lamang ang data hangga't kinakailangan upang:\n•  Magbigay ng serbisyo,\n•  Pahusayin ang AI model,\n•  Matugunan ang pangangailangan sa pananaliksik at batas.\n\nPagkatapos, maaaring ligtas na burahin o gawing anonymous ang data.",
    "7. User Rights" to "7. Mga Karapatan ng Gumagamit",
    "Users may request to:" to "Maaaring hilingin ng gumagamit na:\n•  Ma-access ang personal na impormasyon,\n•  Maitama ang maling impormasyon,\n•  Mabura ang kanilang data,\n•  Bawiin ang pahintulot sa susunod na pagproseso, alinsunod sa batas at pangangailangan ng pananaliksik.",
    "8. Children's Privacy" to "8. Privacy ng mga Bata",
    "BananaQ is not intended" to "Hindi nilalayon ng BananaQ na kusang mangolekta ng personal na impormasyon mula sa batang wala pang labintatlong (13) taong gulang nang walang wastong pahintulot ng magulang o guardian.",
    "9. Consent" to "9. Pahintulot",
    "By using BananaQ, you confirm" to "Sa paggamit ng BananaQ, pinatutunayan mong:\n•  Nauunawaan mo ang mga patakarang ito\n•  Sumasang-ayon ka sa pangongolekta at paggamit ng impormasyong inilalarawan\n•  Gagamitin mo ang application nang responsable at naaayon sa batas",
    "10. Contact" to "10. Pakikipag-ugnayan",
    "For privacy concerns" to "Para sa alalahanin tungkol sa privacy, account, o patakaran, makipag-ugnayan sa BananaQ development team sa opisyal na support channels sa application.\n\nEmail: bananaq@gmail.com",
    "I have read and understood" to "Nabasa at naunawaan ko ang Kasunduan ng Gumagamit, Mga Tuntunin ng Paggamit, at Patakaran sa Privacy sa itaas.",
    "Decline" to "Tanggihan",
    "Submit" to "Isumite"
)

fun localizeAgreementToTagalog(root: View) {
    if (root is TextView) {
        val current = root.text.toString().trim()
        agreementTagalog.firstOrNull { current.startsWith(it.first) }?.let { root.text = it.second }
    }
    if (root is ViewGroup) {
        for (index in 0 until root.childCount) localizeAgreementToTagalog(root.getChildAt(index))
    }
}
