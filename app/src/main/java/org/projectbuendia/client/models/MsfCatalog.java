package org.projectbuendia.client.models;

public interface MsfCatalog extends Catalog {
    Route PO = new Route("PO", "oral", "PO");
    Route IV = new Route("IV", "intravenous", "IV");
    Route SC = new Route("SC", "subcutaneous", "SC");
    Route IM = new Route("IM", "intramuscular", "IM");

    Category ORAL = new Category("DORA", "oral", DosingType.QUANTITY, PO).withDrugs(
        new Drug("DORAABCV", "ABACAVIR sulfate (ABC) [fr:ABACAVIR sulfate (ABC)]").withFormats(
            new Format("DORAABCV3T", "eq. 300 mg base, tab. [fr:éq. 300 mg base, comp.]", Unit.TABLET),
            new Format("DORAABCV6TD", "60 mg, disp. tab. [fr:60 mg, comp. disp.]", Unit.TABLET)
        ),
        new Drug("DORAABLA", "ABC [fr:ABC]").withFormats(
            new Format("DORAABLA1TD", "60 mg / 3TC 30 mg, disp. tab. [fr:60 mg / 3TC 30 mg, comp. disp.]", Unit.TABLET),
            new Format("DORAABLA2T3", "600 mg / 3TC 300 mg, tab. [fr:600 mg / 3TC 300 mg, comp.]", Unit.TABLET),
            new Format("DORAABLA3TD", "120 mg / 3TC 60 mg, disp. tab. [fr:120 mg / 3TC 60 mg, comp. disp.]", Unit.TABLET)
        ),
        new Drug("DORAABLZ", "ABC [fr:ABC]").withFormats(
            new Format("DORAABLZ1T", "60 mg / 3TC 30 mg / AZT 60 mg, tab. [fr:60 mg / 3TC 30 mg / AZT 60 mg, comp.]", Unit.TABLET),
            new Format("DORAABLZ2T", "300 mg / 3TC 150 mg / AZT 300 mg, tab. [fr:300 mg / 3TC 150 mg / AZT 300 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAACEN", "ACENOCOUMAROL [fr:ACENOCOUMAROL]").withFormats(
            new Format("DORAACEN4T", "4 mg, tab. [fr:4 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAACET", "ACETAZOLAMIDE [fr:ACETAZOLAMIDE]").withFormats(
            new Format("DORAACET2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAACIV", "ACICLOVIR [fr:ACICLOVIR]").withFormats(
            new Format("DORAACIV2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAACIV4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET),
            new Format("DORAACIV8T", "800 mg, tab. [fr:800 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAACSA", "ACETYLSALICYLIC acid (aspirin) [fr:Acide ACETYLSALICYLIQUE (aspirine)]").withFormats(
            new Format("DORAACSA3T", "300 mg, tab. [fr:300 mg, comp.]", Unit.TABLET),
            new Format("DORAACSA3TD", "300 mg, disp. tab. [fr:300 mg, comp. disp.]", Unit.TABLET),
            new Format("DORAACSA5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET),
            new Format("DORAACSA7TG", "75 mg, gastro-resistant tab. [fr:75 mg, comp. gastrorés.]", Unit.TABLET)
        ),
        new Drug("DORAALBE", "ALBENDAZOLE [fr:ALBENDAZOLE]").withFormats(
            new Format("DORAALBE1S", "200 mg/5 ml, oral susp., 10 ml, bot. [fr:200 mg/5 ml, susp. orale, 10 ml, fl.]", Unit.ML),
            new Format("DORAALBE2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAALBE4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAALLO", "ALLOPURINOL [fr:ALLOPURINOL]").withFormats(
            new Format("DORAALLO1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAALUM", "ALUMINIUM hydroxide 400 mg/ MAGNESIUM hydrox. 400 mg,chew.tab. [fr:ALUMINIUM hydroxyde 400 mg/ MAGNESIUM hydr. 400 mg,cp.à mâcher]").withFormats(
            new Format("DORAALUM44TC", "", Unit.TABLET)
        ),
        new Drug("DORAAMIO", "AMIODARONE hydrochloride [fr:AMIODARONE chlorhydrate]").withFormats(
            new Format("DORAAMIO2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAMIT", "AMITRIPTYLINE hydrochloride [fr:AMITRIPTYLINE chlorhydrate]").withFormats(
            new Format("DORAAMIT2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAMLO", "AMLODIPINE [fr:AMLODIPINE]").withFormats(
            new Format("DORAAMLO5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAMOC", "AMOXICILLIN [fr:AMOXICILLINE]").withFormats(
            new Format("DORAAMOC1S6", "500 mg/ CLAV.ac. 62.5 mg/5 ml,powder oral susp 60 ml [fr:500 mg/ac.CLAV.62.5 mg/5 ml,poudre susp.orale 60 ml]", Unit.ML),
            new Format("DORAAMOC22TD", "200 mg/ CLAVULANIC acid, 28.5 mg, disp. tab. [fr:200 mg/ acide CLAVULANIQUE 28,5 mg, comp. disp.]", Unit.TABLET),
            new Format("DORAAMOC4S5", "400 mg / CLAV.ac. 57 mg/5 ml, powd.oral susp. 70 ml [fr:400 mg/ ac.CLAV. 57 mg/5 ml,poudre susp.orale 70 ml]", Unit.ML),
            new Format("DORAAMOC56T", "500 mg / CLAVULANIC acid, 62.5 mg, tab. [fr:500 mg/ ac. CLAVULANIQUE 62,5 mg, comp.]", Unit.TABLET),
            new Format("DORAAMOC81T", "875 mg / CLAVULANIC acid 125 mg, tab. [fr:875 mg / ac. CLAVULANIQUE 125 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAMOX", "AMOXICILLIN [fr:AMOXICILLINE]").withFormats(
            new Format("DORAAMOX1S1", "125 mg/5 ml, powder oral susp., 100 ml, bot. [fr:125 mg/5 ml, poudre susp. orale, 100 ml, fl]", Unit.ML),
            new Format("DORAAMOX1S6", "125 mg/5 ml, powder oral susp., 60 ml, bot. [fr:125 mg/5 ml, poudre susp. orale, 60 ml, fl]", Unit.ML),
            new Format("DORAAMOX2C", "250 mg, caps. [fr:250 mg, gél.]", Unit.CAPSULE),
            new Format("DORAAMOX2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORAAMOX2TDB", "250 mg, disp. and breakable tab. [fr:250 mg, comp. disp et sécable]", Unit.TABLET),
            new Format("DORAAMOX5C", "500 mg, caps. [fr:500 mg, gél.]", Unit.CAPSULE),
            new Format("DORAAMOX5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAARLU", "AL [fr:AL]").withFormats(
            new Format("DORAARLU1TD1", "20/120 mg, blister of 6 disp. tab., 5-14 kg [fr:20/120 mg, blister de 6 comp. disp., 5-14 kg]", Unit.TABLET),
            new Format("DORAARLU2TD1", "20/120 mg, blister of 12 disp. tab., 15-24 kg [fr:20/120 mg, blister de 12 comp. disp., 15-24 kg]", Unit.TABLET),
            new Format("DORAARLU3T1", "20/120 mg, blister of 18 tab., 25-34 kg [fr:20/120 mg, blister de 18 comp., 25-34 kg]", Unit.TABLET),
            new Format("DORAARLU4T1", "20/120 mg, blister of 24 tab., >35 kg [fr:20/120 mg, blister de 24 comp., >35 kg]", Unit.TABLET),
            new Format("DORAARLU5T1", "80/480 mg, blister of 6 tab., >35 kg [fr:80/480 mg, blister de 6 comp., >35 kg]", Unit.TABLET)
        ),
        new Drug("DORAASAQ", "AS [fr:AS]").withFormats(
            new Format("DORAASAQ1T1", "25 mg / AQ eq. 67.5 mg base, blister of 3 tab, 4.5-8 kg [fr:25 mg / AQ éq. 67.5 mg base, blister de 3 comp, 4,5-8 kg]", Unit.TABLET),
            new Format("DORAASAQ2T1", "50 mg / AQ eq. 135 mg base, blister of 3 tab, 9-17 kg [fr:50 mg / AQ éq. 135 mg base, blister de 3 comp., 9-17 kg]", Unit.TABLET),
            new Format("DORAASAQ3T1", "100 mg / AQ eq. 270 mg base, blister of 3 tab, 18-35 kg [fr:100 mg / AQ éq. 270 mg base, blister de 3 comp., 18-35 kg]", Unit.TABLET),
            new Format("DORAASAQ4T1", "100 mg / AQ eq. 270 mg base, blister of 6 tab., >36 kg [fr:100 mg / AQ éq. 270 mg base, blister de 6 comp., >36 kg]", Unit.TABLET)
        ),
        new Drug("DORAASCA", "ASCORBIC acid (vitamin C) [fr:Acide ASCORBIQUE (vitamine C)]").withFormats(
            new Format("DORAASCA05T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET),
            new Format("DORAASCA2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORAASCA5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAASMQ", "AS [fr:AS]").withFormats(
            new Format("DORAASMQ1T1", "25 mg / MQ eq. 50 mg base, blister of 3 tab., 5-8 kg [fr:25 mg / MQ éq. 50 mg base, blister de 3 comp., 5-8 kg]", Unit.TABLET),
            new Format("DORAASMQ2T1", "25 mg / MQ eq. 50 mg base, blister of 6 tab., 9-17 kg [fr:25 mg / MQ éq. 50 mg base, blister of 6 comp., 9-17 kg]", Unit.TABLET),
            new Format("DORAASMQ3T1", "100 mg / MQ eq. 200 mg base, blister of 3 tab., 18-29 kg [fr:100 mg / MQ éq. 200 mg base, blister de 3 comp., 18-29 kg]", Unit.TABLET),
            new Format("DORAASMQ4T1", "100 mg / MQ eq. 200 mg base, blister of 6 tab., >30 kg [fr:100 mg / MQ éq. 200 mg base, blister de 6 comp., >30 kg]", Unit.TABLET)
        ),
        new Drug("DORAATAZ", "ATAZANAVIR sulfate (ATV) [fr:ATAZANAVIR sulfate (ATV)]").withFormats(
            new Format("DORAATAZ2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAATOP", "ATOVAQUONE [fr:ATOVAQUONE]").withFormats(
            new Format("DORAATOP1T1", "62.5 mg/ PROGUANIL HCl 25 mg, tab., 11-40 kg [fr:62,5 mg/ PROGUANIL HCl 25 mg, comp, blister,11-40 kg]", Unit.TABLET),
            new Format("DORAATOP2T1", "250 mg / PROGUANIL HCl 100 mg, tab., >40 kg [fr:250 mg / PROGUANIL HCl 100 mg, comp, blister,>40 kg]", Unit.TABLET)
        ),
        new Drug("DORAATOR", "ATORVASTATIN calcium, eq. [fr:ATORVASTATINE calcique]").withFormats(
            new Format("DORAATOR1T", "10 mg base, tab. [fr:10 mg base, comp.]", Unit.TABLET),
            new Format("DORAATOR2T", "20 mg base, tab. [fr:eq. 20 mg base, comp.]", Unit.TABLET),
            new Format("DORAATOR4T", "40 mg base, tab. [fr:eq. 40 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAATVR", "ATV 300 mg / r 100 mg, tab. [fr:ATV 300 mg / r 100 mg, comp.]").withFormats(
            new Format("DORAATVR3T", "", Unit.TABLET)
        ),
        new Drug("DORAAZIT", "AZITHROMYCIN [fr:AZITHROMYCINE]").withFormats(
            new Format("DORAAZIT2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORAAZIT3S", "200 mg/5 ml, powder oral susp., 30 ml, bot. [fr:200 mg/5 ml, poudre susp. orale, 30 ml, fl.]", Unit.ML),
            new Format("DORAAZIT5T", "500 mg, tab [fr:500 mg, comp]", Unit.TABLET)
        ),
        new Drug("DORABECL", "BECLOMETASONE dipropionate [fr:BECLOMETASONE dipropionate]").withFormats(
            new Format("DORABECL1SF", "0.10 mg/puff, 200 puffs,aerosol [fr:0,10 mg/bouffée, 200 b.,aérosol]", Unit.MG),
            new Format("DORABECL2SF", "0.25 mg/puff, 200 puffs,aerosol [fr:0,25 mg/bouffée, 200 b.,aérosol]", Unit.MG),
            new Format("DORABECL5SF", "0.05 mg/puff, 200 puffs,aerosol [fr:0,05 mg/bouffée, 200 b.,aérosol]", Unit.MG)
        ),
        new Drug("DORABEDA", "BEDAQUILINE [fr:BEDAQUILINE]").withFormats(
            new Format("DORABEDA1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORABEND", "BENZNIDAZOLE [fr:BENZNIDAZOLE]").withFormats(
            new Format("DORABEND1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORABIPE", "BIPERIDEN hydrochloride [fr:BIPERIDENE chlorhydrate]").withFormats(
            new Format("DORABIPE2T", "2 mg, tab [fr:2 mg, comp]", Unit.TABLET)
        ),
        new Drug("DORABISA", "BISACODYL [fr:BISACODYL]").withFormats(
            new Format("DORABISA5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORABISO", "BISOPROLOL fumarate [fr:BISOPROLOL fumarate]").withFormats(
            new Format("DORABISO1TB4", "10 mg, break.tab. in 1/4 [fr:10 mg, comp. quadrisécable]", Unit.TABLET),
            new Format("DORABISO2TB", "2.5 mg, break. tab. [fr:2.5 mg, comp. séc.]", Unit.TABLET),
            new Format("DORABISO5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACABG", "CABERGOLINE [fr:CABERGOLINE, 0]").withFormats(
            new Format("DORACABG5TB", "0.5 mg, break. tab. [fr:5 mg, comp. séc.]", Unit.TABLET)
        ),
        new Drug("DORACALC", "CALCIUM carbonate, eq. [fr:CALCIUM carbonate, éq.]").withFormats(
            new Format("DORACALC5TC", "500 mg Ca, chewable tab. [fr:500 mg Ca, comp. à mâcher]", Unit.TABLET),
            new Format("DORACALC6TC", "600 mg Ca, chewable tab. [fr:600 mg Ca, comp. à mâcher]", Unit.TABLET)
        ),
        new Drug("DORACALL", "CALCIUM lactate [fr:CALCIUM lactate]").withFormats(
            new Format("DORACALL3T", "300 mg, eq. to 39 mg Ca, tab. [fr:300 mg, éq. à 39 mg Ca, comp.]", Unit.TABLET)
        ),
        new Drug("DORACARB", "CARBAMAZEPINE [fr:CARBAMAZEPINE]").withFormats(
            new Format("DORACARB2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACARC", "CARBOCISTEINE, 250 mg/5 ml, oral sol. [fr:CARBOCISTEINE, 250 mg/5 ml, sol. orale]").withFormats(
            new Format("DORACARC1S", "200 ml, bot. [fr:200 ml, fl.]", Unit.ML)
        ),
        new Drug("DORACARV", "CARVEDILOL [fr:CAREVEDILOL]").withFormats(
            new Format("DORACARV3TB", "3.125 mg, breakable tab. [fr:3,125 mg, comp. sécable]", Unit.TABLET),
            new Format("DORACARV6TB", "6.25 mg, breakable tab. [fr:6,25 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORACARZ", "CARBIMAZOLE [fr:CARBIMAZOLE]").withFormats(
            new Format("DORACARZ2T", "20 mg, tab. [fr:20 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACEFI", "CEFIXIME [fr:CEFIXIME]").withFormats(
            new Format("DORACEFI1S", "100 mg/5 ml, powder for oral susp., 40 ml, bot. [fr:100 mg/5 ml, poudre pour susp. orale, 40 ml, fl.]", Unit.ML),
            new Format("DORACEFI2S", "100 mg/5 ml, powder for oral susp., 60 ml, bot. [fr:100 mg/5 ml, poudre pour susp. orale, 60 ml, fl.]", Unit.ML),
            new Format("DORACEFI2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORACEFI4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACEFX", "CEFALEXIN [fr:CEFALEXINE]").withFormats(
            new Format("DORACEFX1S", "125 mg/5 ml, granules oral susp., 100 ml, bot. [fr:125 mg/5 ml, granules susp. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORACEFX2C", "250 mg, caps. [fr:250 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORACETI", "CETIRIZINE [fr:CETIRIZINE]").withFormats(
            new Format("DORACETI1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACHAR", "CHARCOAL ACTIVATED, granules for oral susp. [fr:CHARBON ACTIVE, granules pour susp. orale]").withFormats(
            new Format("DORACHAR5G", "50 g, bot. [fr:50 g, fl.]", Unit.MG)
        ),
        new Drug("DORACHLM", "CHLORPROMAZINE hydrochloride, eq. [fr:CHLORPROMAZINE chlorhydrate, éq.]").withFormats(
            new Format("DORACHLM1T", "100 mg base, tab. [fr:100 mg base, comp.]", Unit.TABLET),
            new Format("DORACHLM2T", "25 mg base, tab. [fr:25 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORACHLO", "CHLORAMPHENICOL [fr:CHLORAMPHENICOL]").withFormats(
            new Format("DORACHLO2C", "250 mg, caps. [fr:250 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORACHLQ", "CHLOROQUINE [fr:CHLOROQUINE]").withFormats(
            new Format("DORACHLQ2S1", "eq. 25 mg base/5 ml, syrup, 150 ml, bot. [fr:éq. 25 mg base/5 ml, sirop, 150 ml, fl.]", Unit.ML),
            new Format("DORACHLQ3T", "155 mg base, (250 mg phosphate), tab. [fr:155 mg base, (250 mg phosphate), comp.]", Unit.TABLET)
        ),
        new Drug("DORACIME", "CIMETIDINE [fr:CIMETIDINE]").withFormats(
            new Format("DORACIME2TE", "200 mg, effervescent tab. [fr:200 mg, comp. effervescent]", Unit.TABLET)
        ),
        new Drug("DORACIPR", "CIPROFLOXACIN").withFormats(
            new Format("DORACIPR1S", "CIPROFLOXACIN, 250 mg/5 ml, gran.+ solvent oral susp [fr:CIPROFLOXACINE, 250 mg/5 ml, gran.+ solvant susp.orale]", Unit.ML),
            new Format("DORACIPR2T", "CIPROFLOXACIN hydrochloride, eq. 250 mg base, tab. [fr:CIPROFLOXACINE chlorhydrate, éq. 250 mg base, comp.]", Unit.TABLET),
            new Format("DORACIPR5T", "CIPROFLOXACIN hydrochloride, eq. 500 mg base, tab. [fr:CIPROFLOXACINE chlorhydrate, éq. 500 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORACLAR", "CLARITHROMYCIN").withFormats(
            new Format("DORACLAR1S", "CLARITHROMYCIN 250 mg/5 ml, granules for oral susp., bot. [fr:CLARITHROMYCINE 250 mg/5 ml, granules susp. buv., fl.]", Unit.ML),
            new Format("DORACLAR2T", "CLARITHROMYCIN, 250 mg, tab. [fr:CLARITHROMYCINE, 250 mg, comp.]", Unit.TABLET),
            new Format("DORACLAR5T", "CLARITHROMYCIN, 500 mg, tab. [fr:CLARITHROMYCINE, 500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACLIN", "CLINDAMYCIN hydrochloride, eq. [fr:CLINDAMYCINE chlorhydrate, éq.]").withFormats(
            new Format("DORACLIN1C", "150 mg base, caps. [fr:150 mg base, gél.]", Unit.CAPSULE),
            new Format("DORACLIN3C", "300 mg base, caps. [fr:300 mg base, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORACLOF", "CLOFAZIMINE [fr:CLOFAZIMINE]").withFormats(
            new Format("DORACLOF1C", "100 mg, soft caps. [fr:100 mg, caps. molle]", Unit.CAPSULE),
            new Format("DORACLOF1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET),
            new Format("DORACLOF5C", "50 mg, soft caps. [fr:50 mg, caps. molle]", Unit.CAPSULE),
            new Format("DORACLOF5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACLOP", "CLOPIDOGREL 75 mg, tab. [fr:CLOPIDOGREL 75 mg, comp.]").withFormats(
            new Format("DORACLOP7T", "", Unit.TABLET)
        ),
        new Drug("DORACLOX", "CLOXACILLIN sodium, eq. [fr:CLOXACILLINE sodique, éq.]").withFormats(
            new Format("DORACLOX2C", "250 mg base, caps. [fr:250 mg base, gél.]", Unit.CAPSULE),
            new Format("DORACLOX5C", "500 mg base, caps. [fr:500 mg base, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORACODE", "CODEINE phosphate [fr:CODEINE phosphate]").withFormats(
            new Format("DORACODE1S", "15 mg/5 ml, syrup, 200 ml, bot. [fr:15 mg/5 ml, sirop, 200 ml, fl.]", Unit.ML),
            new Format("DORACODE3T", "30 mg, tab. [fr:30 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACOLC", "COLECALCIFEROL (vit.D3) 10,000 IU/ ml, sol. [fr:COLECALCIFEROL (vit. D3) 10 000 UI/ ml, sol.]").withFormats(
            new Format("DORACOLC1S1", "10 ml, bot. [fr:10 ml, fl.]", Unit.MG)
        ),
        new Drug("DORACOLH", "COLCHICINE [fr:COLCHICINE]").withFormats(
            new Format("DORACOLH1T", "1 mg, tab. [fr:1 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACOTR", "COTRIMOXAZOLE [fr:COTRIMOXAZOLE]").withFormats(
            new Format("DORACOTR1TD", "100 mg / 20 mg, disp. tab. [fr:100 mg / 20 mg, comp. disp.]", Unit.TABLET),
            new Format("DORACOTR2S1", "200 mg/40 mg/5 ml, oral susp,100 ml, bot. [fr:200 mg/40 mg/5 ml, susp orale, 100 ml, fl.]", Unit.ML),
            new Format("DORACOTR4T", "400 mg / 80 mg, tab. [fr:400 mg / 80 mg, comp.]", Unit.TABLET),
            new Format("DORACOTR8T", "800 mg / 160 mg, tab. [fr:800 mg / 160 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACYCL", "CYCLOSERINE [fr:CYCLOSERINE]").withFormats(
            new Format("DORACYCL1C1", "125 mg, caps. blister [fr:125 mg, gél. blister]", Unit.CAPSULE),
            new Format("DORACYCL2C1", "250 mg, caps. blister [fr:250 mg, gél. blister]", Unit.CAPSULE),
            new Format("DORACYCL2C3", "250 mg, caps. bulk [fr:250 mg, gél. vrac]", Unit.CAPSULE)
        ),
        new Drug("DORACYCS", "CYCLIZINE 50 mg, tabs. [fr:CYCLIZINE 50 mg, tabs.]").withFormats(
            new Format("DORACYCS5T", "", Unit.MG)
        ),
        new Drug("DORADACL", "DACLATASVIR dihydrochloride (DCV), eq. [fr:DACLATASVIR dichlorhydrate (DCV), éq.]").withFormats(
            new Format("DORADACL3T", "30 mg base, tab. [fr:30 mg base, comp.]", Unit.TABLET),
            new Format("DORADACL6T", "60 mg base, tab. [fr:60 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORADAPS", "DAPSONE [fr:DAPSONE]").withFormats(
            new Format("DORADAPS1TB", "100 mg, break. tab. [fr:100 mg, comp. sécable]", Unit.TABLET),
            new Format("DORADAPS2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET),
            new Format("DORADAPS5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADARU", "DARUNAVIR ethanolate (DRV), eq. [fr:DARUNAVIR éthanolate (DRV), éq.]").withFormats(
            new Format("DORADARU1T", "150 mg base, tab. [fr:150 mg base, comp.]", Unit.TABLET),
            new Format("DORADARU3T", "300 mg base, tab. [fr:300 mg base, comp.]", Unit.TABLET),
            new Format("DORADARU4T", "400 mg base, tab. [fr:400 mg base, comp.]", Unit.TABLET),
            new Format("DORADARU6T", "600 mg base, tab. [fr:600 mg base, comp.]", Unit.TABLET),
            new Format("DORADARU7T", "75 mg base, tab. [fr:75 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORADEFP", "DEFERIPRONE [fr:DEFERIPRONE]").withFormats(
            new Format("DORADEFP2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORADEFP5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADEFS", "DEFERASIROX [fr:DEFERASIROX]").withFormats(
            new Format("DORADEFS1TD", "125 mg, disp. tab. [fr:125 mg, comp. disp.]", Unit.TABLET),
            new Format("DORADEFS2TD", "250 mg, disp. tab. [fr:250 mg, comp. disp.]", Unit.TABLET),
            new Format("DORADEFS5TD", "500 mg, disp. tab. [fr:500 mg, comp. disp.]", Unit.TABLET)
        ),
        new Drug("DORADELA", "DELAMANID [fr:DELAMANID]").withFormats(
            new Format("DORADELA5T1", "50 mg, tab., blister [fr:50 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORADESO", "DESOGESTREL 0.075 mg, blister of 28 tab. [fr:DESOGESTREL 0]").withFormats(
            new Format("DORADESO7T1", " [fr:075 mg, blister de 28 comp.]", Unit.TABLET)
        ),
        new Drug("DORADHAP", "DHA [fr:DHA]").withFormats(
            new Format("DORADHAP1T1", "20 mg / PPQ 160 mg, blister de 3 tab., 5-12 kg [fr:20 mg / PPQ 160 mg, blister de 3 comp., 5-12 kg]", Unit.TABLET),
            new Format("DORADHAP2T1", "40 mg / PPQ 320 mg, blister of 3 tab., 13-23 kg [fr:40 mg / PPQ 320 mg, blister de 3 comp., 13-23 kg]", Unit.TABLET),
            new Format("DORADHAP3T1", "40 mg / PPQ 320 mg, blister of 6 tab., 24-34 kg [fr:40 mg / PPQ 320 mg, blister de 6 comp., 24-34 kg]", Unit.TABLET),
            new Format("DORADHAP4T1", "40 mg / PPQ 320 mg, blister of 9 tab., 35-74 kg [fr:40 mg / PPQ 320 mg, blister de 9 comp., 35-74 kg]", Unit.TABLET),
            new Format("DORADHAP5T1", "40 mg / PPQ 320 mg, blister de 12 tab., 75-100 kg [fr:40 mg / PPQ 320 mg, blister de 12 comp., 75-100 kg]", Unit.TABLET)
        ),
        new Drug("DORADIAC", "DIACETYLCYSTEINATE methyle [fr:DIACETYLCYSTEINE méthyl]").withFormats(
            new Format("DORADIAC2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADIAZ", "DIAZEPAM [fr:DIAZEPAM]").withFormats(
            new Format("DORADIAZ2T", "2 mg, tab. [fr:2 mg, comp.]", Unit.TABLET),
            new Format("DORADIAZ5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADICL", "DICLOFENAC sodium [fr:DICLOFENAC sodique]").withFormats(
            new Format("DORADICL2TG", "25 mg, gastro-resistant tab. [fr:25 mg, comp. gastro-résistant]", Unit.TABLET)
        ),
        new Drug("DORADIET", "DIETHYLCARBAMAZINE citrate [fr:DIETHYLCARBAMAZINE citrate]").withFormats(
            new Format("DORADIET1TB", "eq. 100 mg base, break. tab. [fr:éq.100 mg base, comp. séc.]", Unit.TABLET)
        ),
        new Drug("DORADIGO", "DIGOXIN [fr:DIGOXINE]").withFormats(
            new Format("DORADIGO2T", "0.25 mg, tab. [fr:0,25 mg, comp.]", Unit.TABLET),
            new Format("DORADIGO6T", "0.0625 mg, tab. [fr:0,0625 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADIPH", "DIPHENHYDRAMINE [fr:DIPHENHYDRAMINE]").withFormats(
            new Format("DORADIPH9T", "90 mg, tab. [fr:90 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADOLU", "DOLUTEGRAVIR sodium (DTG) [fr:DOLUTEGRAVIR sodium (DTG)]").withFormats(
            new Format("DORADOLU5T", "eq. 50 mg base, tab. [fr:éq. 50 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORADOXY", "DOXYCYCLINE salt [fr:DOXYCYCLINE sel]").withFormats(
            new Format("DORADOXY1T", "eq. 100 mg base, tab. [fr:éq. 100 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAEFAV", "EFAVIRENZ (EFV) [fr:EFAVIRENZ (EFV)]").withFormats(
            new Format("DORAEFAV2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE),
            new Format("DORAEFAV2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAEFAV2TB", "200 mg, break. tab. [fr:200 mg, comp. séc.]", Unit.TABLET),
            new Format("DORAEFAV6T", "600 mg, tab. [fr:600 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAEHRI", "E 275 mg / H 75 mg / R 150 mg, tab. [fr:E 275 mg / H 75 mg / R 150 mg, comp.]").withFormats(
            new Format("DORAEHRI1T1", "blister [fr:blister]", Unit.TABLET),
            new Format("DORAEHRI1T3", "bulk [fr:vrac]", Unit.TABLET)
        ),
        new Drug("DORAEHZR", "E 275 mg / H 75 mg / Z 400 mg / R 150 mg, tab. [fr:E 275 mg / H 75 mg / Z 400 mg / R 150 mg, comp.]").withFormats(
            new Format("DORAEHZR2T1", "blister [fr:blister]", Unit.TABLET),
            new Format("DORAEHZR2T3", "bulk [fr:vrac]", Unit.TABLET)
        ),
        new Drug("DORAENAL", "ENALAPRIL maleate [fr:ENALAPRIL maléate]").withFormats(
            new Format("DORAENAL2T", "20 mg, tab. [fr:20 mg, comp.]", Unit.TABLET),
            new Format("DORAENAL5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAERYT", "ERYTHROMYCIN [fr:ERYTHROMYCINE]").withFormats(
            new Format("DORAERYT1G", "ethylsucc. 125 mg, gran. for oral susp., sachet [fr:ethylsucc,125 mg, gran. pour susp.orale, sachet]", Unit.MG),
            new Format("DORAERYT1S1", "ethylsucc. 125 mg/5 ml,powder oral susp.100 ml,bot [fr:ethylsucc,125 mg/5 ml,poudre susp.orale,100 ml,fl]", Unit.ML),
            new Format("DORAERYT2T", "stearate, eq. 250 mg base, tab. [fr:stéarate, eq. 250 mg base, comp.]", Unit.TABLET),
            new Format("DORAERYT5T", "stearate, eq. 500 mg base, tab. [fr:stéarate, eq. 500 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAETHA", "ETHAMBUTOL hydrochloride (E), eq. [fr:ETHAMBUTOL chlorhydrate (E), éq.]").withFormats(
            new Format("DORAETHA1T1", "100 mg base, tab. blister [fr:100 mg base, comp. blister]", Unit.TABLET),
            new Format("DORAETHA1T3", "100 mg base, tab. bulk [fr:100 mg base, comp. vrac]", Unit.TABLET),
            new Format("DORAETHA4T1", "400 mg base, tab. blister [fr:400 mg base, comp. blister]", Unit.TABLET),
            new Format("DORAETHA4T3", "400 mg base, tab. bulk [fr:400 mg base, comp. vrac]", Unit.TABLET)
        ),
        new Drug("DORAETHL", "ETHINYLESTR. 0.03 mg / LEVONORGESTREL 0.15 mg, blister 28tab [fr:ETHINYLESTR. 0,03 mg / LEVONORGESTREL 0]").withFormats(
            new Format("DORAETHL31T", " [fr:15 mg, plaq. 28 comp.]", Unit.MG)
        ),
        new Drug("DORAETHN", "ETHIONAMIDE [fr:ETHIONAMIDE]").withFormats(
            new Format("DORAETHN1T1", "125 mg, tab., blister [fr:125 mg, comp., blister]", Unit.TABLET),
            new Format("DORAETHN1TD1", "125 mg, dispersible tab., blister [fr:125 mg, comp. dispersible, blister]", Unit.TABLET),
            new Format("DORAETHN2T1", "250 mg, tab., blister [fr:250 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORAETRA", "ETRAVIRINE (ETV) [fr:ETRAVIRINE (ETV)]").withFormats(
            new Format("DORAETRA1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAFENO", "FENOFIBRATE [fr:FENOFIBRATE]").withFormats(
            new Format("DORAFENO2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAFERF", "FERROUS salt / FOLIC acid [fr:sel de FER éq.]").withFormats(
            new Format("DORAFERF14T", "60 mg iron / FOLIC acid 0.4 mg, tab [fr:60 mg fer / acide FOLIQUE 0,4 mg, comp.]", Unit.TABLET),
            new Format("DORAFERF24T", "65 mg iron / FOLIC acid 0.4 mg, tab [fr:65 mg fer / acide FOLIQUE 0,4 mg, comp.]", Unit.TABLET),
            new Format("DORAFERF45CP", "47 mg iron / FOLIC acid 0.5 mg,prol.rel.caps. [fr:47 mg fer / acide FOLIQUE 0,5 mg, gél.lib.prol.]", Unit.CAPSULE)
        ),
        new Drug("DORAFERS", "FERROUS salt").withFormats(
            new Format("DORAFERS2S", "FERROUS salt, eq. iron 45 mg/5 ml, syrup, 200 ml, bot. [fr:sel de FER, éq. 45 mg/5 ml fer, sirop, 200 ml, fl.]", Unit.ML),
            new Format("DORAFERS2T", "FERROUS salt, eq. +/- 65 mg iron, tab. [fr:sel de FER, éq. +/- 65 mg de fer, comp.]", Unit.TABLET),
            new Format("DORAFERS3S", "FERROUS salt, eq. iron 45 mg/5 ml, syrup, 300 ml, bot. [fr:sel de FER, éq. 45 mg/5 ml fer, sirop, 300 ml, fl.]", Unit.ML),
            new Format("DORAFERS4S", "sodium FEREDETATE, eq. 34 mg/5 ml iron, 125 ml, bot. [fr:FEREDETATE de sodium, eq. 34 mg/5 ml fer, 125 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAFEXI", "FEXINIDAZOLE, 600 mg, wallet of [fr:FEXINIDAZOLE, 600 mg, wallet of]").withFormats(
            new Format("DORAFEXI6T1A", "24 tabs. , >34 kg [fr:24 tabs. , >34 kg]", Unit.MG),
            new Format("DORAFEXI6T1P", "14 tabs., 20-34 kg [fr:14 tabs., 20-34 kg]", Unit.MG)
        ),
        new Drug("DORAFLUC", "FLUCONAZOLE [fr:FLUCONAZOLE]").withFormats(
            new Format("DORAFLUC1C", "100 mg, caps. [fr:100 mg, gél.]", Unit.CAPSULE),
            new Format("DORAFLUC1S", "50 mg/5 ml, powder oral susp., bot. [fr:50 mg/5 ml, poudre susp. orale, fl.]", Unit.ML),
            new Format("DORAFLUC2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE),
            new Format("DORAFLUC2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAFLUC5C", "50 mg, caps. [fr:50 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAFLUS", "FLUTICASONE 125µg/SALMETEROL 25µg /puff, aerosol [fr:FLUTICASONE 125µg/SALMETEROL 25µg /bouffée, aérosol]").withFormats(
            new Format("DORAFLUS12SF", "", Unit.MG)
        ),
        new Drug("DORAFLUT", "FLUTICASONE propionate, 50µg/puff, aerosol [fr:FLUTICASONE propionate, 50µg/bouffée, aérosol]").withFormats(
            new Format("DORAFLUT5SF", "120 doses [fr:120 doses]", Unit.MG)
        ),
        new Drug("DORAFLUX", "FLUOXETINE hydrochloride [fr:FLUOXETINE chlorhydrate]").withFormats(
            new Format("DORAFLUX2C", "eq. 20 mg base, caps. [fr:éq. 20 mg base, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAFLUY", "FLUCYTOSINE [fr:FLUCYTOSINE]").withFormats(
            new Format("DORAFLUY5T", "500 mg, tab. [fr:500 mg , comp.]", Unit.TABLET)
        ),
        new Drug("DORAFOLA", "FOLIC acid [fr:Acide FOLIQUE]").withFormats(
            new Format("DORAFOLA5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAFOLC", "CALCIUM FOLINATE, eq. [fr:FOLINATE de CALCIUM, éq.]").withFormats(
            new Format("DORAFOLC1T", "15 mg, folinic acid, tab. [fr:15 mg, acide folinique, comp.]", Unit.TABLET),
            new Format("DORAFOLC2C", "25 mg folinic acid, caps. [fr:25 mg acide folinique, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAFOSF", "FOSFOMYCIN trometamol [fr:FOSFOMYCINE trométamol]").withFormats(
            new Format("DORAFOSF3S", "eq. 3 g base, sachet [fr:éq. 3 g base, sachet]", Unit.MG)
        ),
        new Drug("DORAFURO", "FUROSEMIDE [fr:FUROSEMIDE]").withFormats(
            new Format("DORAFURO2T", "20 mg, tab. [fr:20 mg, comp.]", Unit.TABLET),
            new Format("DORAFURO4T", "40 mg, tab. [fr:40 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAGABA", "GABAPENTIN [fr:GABAPENTINE]").withFormats(
            new Format("DORAGABA1C", "100 mg caps. [fr:100 mg gél.]", Unit.CAPSULE),
            new Format("DORAGABA3C", "300 mg caps. [fr:300 mg gél.]", Unit.CAPSULE),
            new Format("DORAGABA4C", "400 mg, caps. [fr:400 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAGLIB", "GLIBENCLAMIDE [fr:GLIBENCLAMIDE]").withFormats(
            new Format("DORAGLIB5TB", "5 mg, breakable tab. [fr:5 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORAGLIC", "GLICLAZIDE [fr:GLICLAZIDE]").withFormats(
            new Format("DORAGLIC8TB", "80 mg, breakable tab. [fr:80 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORAGLIM", "GLIBENCLAMIDE 5 mg / METFORMIN hydrochloride [fr:GLIBENCLAMIDE 5 mg / METFORMINE chlorhydrate]").withFormats(
            new Format("DORAGLIM55T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAGLYT", "GLYCERYL TRINITRATE [fr:GLYCERYLE TRINITRATE, 0]").withFormats(
            new Format("DORAGLYT5T", "0.5 mg, sublingual tab. [fr:5 mg, comp. sublingual]", Unit.TABLET)
        ),
        new Drug("DORAGRIS", "GRISEOFULVIN [fr:GRISEOFULVINE]").withFormats(
            new Format("DORAGRIS1T", "125 mg, tab. [fr:125 mg, comp.]", Unit.TABLET),
            new Format("DORAGRIS5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHALO", "HALOTHANE [fr:HALOTHANE]").withFormats(
            new Format("DORAHALO1A2", "250 ml, bot. [fr:250 ml, fl.]", Unit.MG)
        ),
        new Drug("DORAHALP", "").withFormats(
            new Format("DORAHALP05C", "HALOPERIDOL 0.5 mg, caps. [fr:HALOPERIDOL 0,5 mg, gél.]", Unit.CAPSULE),
            new Format("DORAHALP05T", "HALOPERIDOL 0.5 mg, tab. [fr:HALOPERIDOL 0,5 mg, comp.]", Unit.TABLET),
            new Format("DORAHALP1S2", "HALOPERIDOL, 2 mg/ ml, oral sol., 100 ml, bot. with pipette [fr:HALOPERIDOL, 2 mg/ ml, sol.orale, 100 ml, fl. avec pipette]", Unit.MG),
            new Format("DORAHALP1T", "HALOPERIDOL, 1 mg, tab. [fr:HALOPERIDOL, 1 mg, comp.]", Unit.TABLET),
            new Format("DORAHALP3D", "HALOPERIDOL, 2 mg/ ml/20 drops, 30 ml, bot. [fr:HALOPERIDOL, 2 mg/ ml/20 gouttes, 30 ml, fl.]", Unit.MG),
            new Format("DORAHALP5T", "HALOPERIDOL, 5 mg, tab. [fr:HALOPERIDOL, 5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHPST", "INH 300 mg/ PYRIDOXINE 25 mg/ SMX 800 mg / TMP 160 mg, tab. [fr:INH 300 mg/ PYRIDOXINE 25 mg/ SMX 800 mg / TMP 160 mg, comp.]").withFormats(
            new Format("DORAHPST32T", "", Unit.TABLET)
        ),
        new Drug("DORAHRIF", "H [fr:H]").withFormats(
            new Format("DORAHRIF5TD1", "50 mg / R 75 mg, disp. tab., blister [fr:50 mg / R 75 mg, comp. disp., blister]", Unit.TABLET),
            new Format("DORAHRIF6TD1", "60 mg / R 60 mg, disp. tab., blister [fr:60 mg / R 60 mg, comp. disp., blister]", Unit.TABLET),
            new Format("DORAHRIF6TD3", "60 mg / R 60 mg, disp. tab., bulk [fr:60 mg / R 60 mg, comp. disp., vrac]", Unit.TABLET),
            new Format("DORAHRIF7T1", "75 mg / R 150 mg, tab., blister [fr:75 mg / R 150 mg, comp., blister]", Unit.TABLET),
            new Format("DORAHRIF7T3", "75 mg / R 150 mg, tab., bulk [fr:75 mg / R 150 mg, comp., vrac]", Unit.TABLET)
        ),
        new Drug("DORAHYDC", "HYDROXYCARBAMIDE [fr:HYDROXYCARBAMIDE]").withFormats(
            new Format("DORAHYDC1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHYDO", "HYDROCHLOROTHIAZIDE [fr:HYDROCHLOROTHIAZIDE]").withFormats(
            new Format("DORAHYDO1T", "12.5 mg, tab. [fr:12,5 mg, comp.]", Unit.TABLET),
            new Format("DORAHYDO2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHYDX", "HYDROXYZINE dihydrochloride [fr:HYDROXYZINE dichlorhydrate]").withFormats(
            new Format("DORAHYDX2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHYOS", "HYOSCINE BUTYLBROMIDE (scopolamine butylbromide) [fr:BUTYLBROMURE HYOSCINE (butylbromure scopolamine)]").withFormats(
            new Format("DORAHYOS1T", "10 mg, tab [fr:10 mg, cp]", Unit.TABLET)
        ),
        new Drug("DORAHZRI", "H 50 mg / Z 150 mg / R 75 mg, disp. tab., blister [fr:H 50 mg / Z 150 mg / R 75 mg, comp. disp., blister]").withFormats(
            new Format("DORAHZRI5TD1", "", Unit.TABLET)
        ),
        new Drug("DORAIBUP", "IBUPROFEN [fr:IBUPROFENE]").withFormats(
            new Format("DORAIBUP2S", "100 mg/5 ml, oral susp., 150 ml, bot. [fr:100 mg/5 ml, susp. orale, 150 ml, fl.]", Unit.ML),
            new Format("DORAIBUP2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAIBUP3S", "100 mg/5 ml, oral susp., 200 ml, bot. [fr:100 mg/5 ml, susp. orale, 200 ml, fl.]", Unit.ML),
            new Format("DORAIBUP4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAIODO", "IODIZED OIL [fr:HUILE IODEE]").withFormats(
            new Format("DORAIODO1C", "190 mg, caps. [fr:190 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAIPRA", "IPRATROPIUM bromide [fr:IPRATROPIUM bromure]").withFormats(
            new Format("DORAIPRA2N", "0.250 mg/ ml, 1 ml, sol. for nebuliser [fr:0.250 mg/ ml, 1 ml, sol. pour nébuliseur]", Unit.MG),
            new Format("DORAIPRA2N2", "0.125 mg/ ml, 2 ml, sol. for nebuliser [fr:0,125 mg/ ml, 2 ml, sol. pour nébuliseur]", Unit.MG),
            new Format("DORAIPRA2SF", "20µg/puff, 200 puffs, aerosol [fr:20µg/bouffée, 200 bouffées, aerosol]", Unit.MG),
            new Format("DORAIPRA5N", "0.250 mg/ ml, 2 ml, sol. for nebuliser [fr:0,250 mg/ ml, 2 ml, sol. pour nébuliseur]", Unit.MG)
        ),
        new Drug("DORAISOB", "ISOSORBIDE DINITRATE [fr:ISOSORBIDE DINITRATE]").withFormats(
            new Format("DORAISOB1T", "10 mg, tab [fr:10 mg, comp]", Unit.TABLET),
            new Format("DORAISOB5T", "5 mg, sublingual tab. [fr:5 mg, comp. sublingual]", Unit.TABLET)
        ),
        new Drug("DORAISOF", "ISOFLURANE, liquid [fr:ISOFLURANE, liquide]").withFormats(
            new Format("DORAISOF2L", "250 ml, bot. [fr:250 ml, fl.]", Unit.MG)
        ),
        new Drug("DORAISON", "ISONIAZID (H) [fr:ISONIAZIDE (H)]").withFormats(
            new Format("DORAISON1T1", "100 mg, breakable tab., blister [fr:100 mg, comp. sécable, blister]", Unit.TABLET),
            new Format("DORAISON1T3", "100 mg, breakable tab., bulk [fr:100 mg, comp. sécable, vrac]", Unit.TABLET),
            new Format("DORAISON3T1", "300 mg, tab., blister [fr:300 mg, comp., blister]", Unit.TABLET),
            new Format("DORAISON3T3", "300 mg, tab., bulk [fr:300 mg, comp., vrac]", Unit.TABLET),
            new Format("DORAISON5S", "50 mg/5 ml, oral sol., 500 ml, bot. [fr:50 mg/5 ml, sol. orale, 500 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAISOS", "ISOSORBIDE DINITRATE [fr:ISOSORBIDE DINITRATE]").withFormats(
            new Format("DORAISOS2T", "20 mg, prol.release, tab. [fr:20 mg, libér.prol., comp.]", Unit.TABLET)
        ),
        new Drug("DORAITRA", "ITRACONAZOLE [fr:ITRACONAZOLE]").withFormats(
            new Format("DORAITRA1C", "100 mg, caps. [fr:100 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAIVER", "IVERMECTIN [fr:IVERMECTINE]").withFormats(
            new Format("DORAIVER3T", "(onchocerciasis, mass distribution), 3 mg, tab. [fr:(onchocercose, distribution de masse),3 mg, comp]", Unit.TABLET),
            new Format("DORAIVER3T4", "(onchocerciasis), 3 mg, tab. [fr:(onchocercose), 3 mg, comp.]", Unit.TABLET),
            new Format("DORAIVER3TS", "(scabies + other indic.), 3 mg, tab. [fr:(gale + autres indic.), 3 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALABE", "LABETALOL hydrochloride [fr:LABETALOL chlorhydrate]").withFormats(
            new Format("DORALABE1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET),
            new Format("DORALABE2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALACT", "LACTULOSE [fr:LACTULOSE]").withFormats(
            new Format("DORALACT1S", "min. 3.1 g/5 ml, oral sol., bot. [fr:min. 3,1g/5 ml, sol. orale, fl.]", Unit.ML),
            new Format("DORALACT3S", "10g/15 ml, oral sol., sachet [fr:10g/15 ml, sol. orale, sachet]", Unit.ML)
        ),
        new Drug("DORALAMI", "LAMIVUDINE (3TC) [fr:LAMIVUDINE (3TC)]").withFormats(
            new Format("DORALAMI1S", "50 mg/5 ml, oral sol., 100 ml, bot. [fr:50 mg/5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORALAMI1T", "150 mg, tab. [fr:150 mg, comp.]", Unit.TABLET),
            new Format("DORALAMI2S", "50 mg/5 ml, oral sol., 240 ml, bot. [fr:50 mg/5 ml, sol. orale, 240 ml, fl.]", Unit.ML)
        ),
        new Drug("DORALEFX", "LEVOFLOXACIN [fr:LEVOFLOXACINE]").withFormats(
            new Format("DORALEFX1TD1", "100 mg, dispersible tab., blister [fr:100 mg, comp. dispersible, blister]", Unit.TABLET),
            new Format("DORALEFX2T", "250 mg, tab., blister [fr:250 mg, comp., blister]", Unit.TABLET),
            new Format("DORALEFX5T", "500 mg, tab., blister [fr:500 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORALESO", "LEDIPASVIR 90 mg / SOFOSBUVIR 400 mg, tab. [fr:LEDIPASVIR 90 mg / SOFOSBUVIR 400 mg, comp.]").withFormats(
            new Format("DORALESO94T", "", Unit.TABLET)
        ),
        new Drug("DORALEVC", "LEVODOPA 250 mg / CARBIDOPA 25 mg, tab. [fr:LEVODOPA 250 mg / CARBIDOPA 25 mg, comp.]").withFormats(
            new Format("DORALEVC2T", "", Unit.TABLET)
        ),
        new Drug("DORALEVE", "LEVETIRACETAM, 500 mg/5 ml, oral sol. [fr:LEVETIRACETAM, 500 mg/5 ml, sol. orale]").withFormats(
            new Format("DORALEVE1S3", "300 ml bot. [fr:300 ml fl.]", Unit.ML)
        ),
        new Drug("DORALEVN", "LEVONORGESTREL [fr:LEVONORGESTREL]").withFormats(
            new Format("DORALEVN1T", "1.5 mg, tab. [fr:1,5 mg, comp.]", Unit.TABLET),
            new Format("DORALEVN3T1", "0.03 mg, blister of 35 tab. [fr:0,03 mg, blister de 35 comp.]", Unit.TABLET)
        ),
        new Drug("DORALEVO", "LEVOTHYROXINE sodium [fr:LEVOTHYROXINE sodique]").withFormats(
            new Format("DORALEVO1T", "0.1 mg, tab. [fr:0,1 mg, cp]", Unit.TABLET),
            new Format("DORALEVO2T", "0.025 mg, tab. [fr:0.025 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALINE", "LINEZOLID [fr:LINEZOLIDE]").withFormats(
            new Format("DORALINE1S", "100 mg/5 ml, granules for oral susp., 150 ml, bot. [fr:100 mg/5 ml, granules susp. orale, 150 ml, fl.]", Unit.ML),
            new Format("DORALINE6T", "600 mg, tab. [fr:600 mg, comp.]", Unit.TABLET),
            new Format("DORALINE6TB1", "600 mg, breakable tab., blister [fr:600 mg, comp. sécable, blister]", Unit.TABLET)
        ),
        new Drug("DORALOPE", "LOPERAMIDE hydrochloride, 2 mg [fr:LOPERAMIDE chlorhydrate, 2 mg]").withFormats(
            new Format("DORALOPE2C", "caps. [fr:gél.]", Unit.CAPSULE),
            new Format("DORALOPE2T", "tab. [fr:comp.]", Unit.TABLET)
        ),
        new Drug("DORALORA", "LORATADINE [fr:LORATADINE]").withFormats(
            new Format("DORALORA1S", "5 mg/5 ml, oral sol., 100 ml, bot. [fr:5 mg/5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORALORA1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALOSA", "LOSARTAN potassium, 50 mg [fr:LOSARTAN potassium, 50 mg]").withFormats(
            new Format("DORALOSA5T", "tab. [fr:comp.]", Unit.TABLET),
            new Format("DORALOSA5TB", "breakable tab. [fr:comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORALPVR", "LPV [fr:LPV]").withFormats(
            new Format("DORALPVR1G", "40 mg / r 10 mg, granules, sachet [fr:40 mg / r 10 mg, granules, sachet]", Unit.MG),
            new Format("DORALPVR1P", "40 mg / r 10 mg, pellets-in-a-capsule [fr:40 mg / r 10 mg, granules dans gélule]", Unit.MG),
            new Format("DORALPVR2S", "/ r 400/100 mg/5 ml, oral sol., 60 ml, bot. [fr:/ r 400/100 mg/5 ml, sol.orale, 60 ml, fl.]", Unit.ML),
            new Format("DORALPVR3S", "/ r 400/100 mg/5 ml, oral sol., 160 ml, bot. [fr:/ r 400/100 mg/5 ml, sol. orale, 160 ml, fl.]", Unit.ML),
            new Format("DORALPVR4T", "100 mg / r 25 mg, tab. [fr:100 mg / r 25 mg, comp.]", Unit.TABLET),
            new Format("DORALPVR5T", "200 mg / r 50 mg, tab. [fr:200 mg / r 50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMAGN", "MAGNESIUM oxide [fr:Oxyde de MAGNESIUM]").withFormats(
            new Format("DORAMAGN1TE", "270 mg, eq. to 150 mg Mg, efferv.tab. [fr:270 mg, éq. 150 mg Mg, comp. efferv.]", Unit.TABLET),
            new Format("DORAMAGN3T", "eq. to 300 mg Mg, tab. [fr:éq. à 300 mg Mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMAGP", "MAGNESIUM lactate [fr:MAGNESIUM lactate]").withFormats(
            new Format("DORAMAGP55T", "eq. 48 mg Mg / PYRIDOXINE HCl 5 mg, tab. [fr:éq. 48 mg Mg / PYRIDOXINE HCl 5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMEBE", "MEBENDAZOLE [fr:MEBENDAZOLE]").withFormats(
            new Format("DORAMEBE1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMEDR", "MEDROXYPROGESTERONE acetate [fr:MEDROXYPROGESTERONE acétate]").withFormats(
            new Format("DORAMEDR1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMEFL", "MEFLOQUINE hydrochloride [fr:MEFLOQUINE chlorhydrate]").withFormats(
            new Format("DORAMEFL2T", "eq. 250 mg base, tab. [fr:éq. 250 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMETF", "METFORMIN hydrochloride [fr:METFORMINE chlorhydrate]").withFormats(
            new Format("DORAMETF1T", "1000 mg, tab. [fr:1000 mg, comp.]", Unit.TABLET),
            new Format("DORAMETF5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET),
            new Format("DORAMETF8T", "850 mg, tab. [fr:850 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMETN", "").withFormats(
            new Format("DORAMETN2S", "METRONIDAZOLE benzoate, eq.200 mg/5 ml base, oral susp., 100 ml [fr:METRONIDAZOLE benzoate, éq.200 mg/5 ml base, susp. orale,100 ml]", Unit.ML),
            new Format("DORAMETN2T", "METRONIDAZOLE, 250 mg, tab. [fr:METRONIDAZOLE, 250 mg, comp.]", Unit.TABLET),
            new Format("DORAMETN5T", "METRONIDAZOLE, 500 mg, tab. [fr:METRONIDAZOLE, 500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMETO", "METOCLOPRAMIDE hydrochloride anhydrous [fr:METOCLOPRAMIDE chlorhydrate anhydre]").withFormats(
            new Format("DORAMETO1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMETY", "METHYLDOPA [fr:METHYLDOPA]").withFormats(
            new Format("DORAMETY2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMICO", "").withFormats(
            new Format("DORAMICO2J1", "MICONAZOLE nitrate, 2%, oral gel, 15g, tube [fr:MICONAZOLE nitrate, 2%, gel oral, 15g, tube]", Unit.ML),
            new Format("DORAMICO2J4", "MICONAZOLE, 2%, oral gel, 40g, tube [fr:MICONAZOLE, 2%, gel oral, 40g, tube]", Unit.ML),
            new Format("DORAMICO2J8", "MICONAZOLE, 2%, oral gel, 80g, tube [fr:MICONAZOLE, 2%, gel oral, 80g, tube]", Unit.ML)
        ),
        new Drug("DORAMIFP", "MIFEPRISTONE [fr:MIFEPRISTONE]").withFormats(
            new Format("DORAMIFP2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMILT", "MILTEFOSINE [fr:MILTEFOSINE]").withFormats(
            new Format("DORAMILT1C", "10 mg, caps. [fr:10 mg, gél.]", Unit.CAPSULE),
            new Format("DORAMILT5C", "50 mg, caps. [fr:50 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAMISP", "MISOPROSTOL [fr:MISOPROSTOL]").withFormats(
            new Format("DORAMISP25T", "25 µg, tab. [fr:25 µg, comp.]", Unit.TABLET),
            new Format("DORAMISP2T", "200 µg, tab. [fr:200 µg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMMNS", "MULTIPLE MICRONUTRIENTS SUPPLEMENTS, tab. [fr:MICRONUTRIMENTS MULTIPLES, SUPPLEMENTS, comp.]").withFormats(
            new Format("DORAMMNS1T", "", Unit.TABLET)
        ),
        new Drug("DORAMONT", "MONTELUKAST [fr:MONTELUKAST]").withFormats(
            new Format("DORAMONT5TC", "5 mg, chewing tab. [fr:5 mg, tab. à macher]", Unit.TABLET)
        ),
        new Drug("DORAMORP", "MORPHINE sulfate [fr:MORPHINE sulfate]").withFormats(
            new Format("DORAMORP1CS", "10 mg, prolonged-release caps. [fr:10 mg, gél. libération prolongée]", Unit.CAPSULE),
            new Format("DORAMORP1S", "10 mg/5 ml, oral sol., 100 ml, bot. [fr:10 mg/5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORAMORP1T", "10 mg, immediate release breakable tab. [fr:10 mg, comp. sécable libération immédiate]", Unit.TABLET),
            new Format("DORAMORP1TS", "10 mg, prolonged-release tab. [fr:10 mg, comp. libération prolongée]", Unit.TABLET),
            new Format("DORAMORP3CS", "30 mg, prolonged-release caps. [fr:30 mg, gél. libération prolongée]", Unit.CAPSULE),
            new Format("DORAMORP3TS", "30 mg, prolonged-release, tab. [fr:30 mg, comp. libération prolongée]", Unit.TABLET)
        ),
        new Drug("DORAMOXI", "MOXIFLOXACIN hydrochloride [fr:MOXIFLOXACINE chlorhydrate]").withFormats(
            new Format("DORAMOXI1TD", "eq. 100 mg base, disp. tab. [fr:éq. 100 mg base, comp. disp.]", Unit.TABLET),
            new Format("DORAMOXI4T1", "eq 400 mg base, tab. blister [fr:éq 400 mg base, comp. blister]", Unit.TABLET)
        ),
        new Drug("DORAMULT", "MULTIVITAMINS, tab. [fr:MULTIVITAMINES, comp.]").withFormats(
            new Format("DORAMULT1T", "", Unit.TABLET)
        ),
        new Drug("DORANEVI", "NEVIRAPINE (NVP) [fr:NEVIRAPINE (NVP)]").withFormats(
            new Format("DORANEVI1S1", "50 mg/5 ml, oral susp., 100 ml, bot. [fr:50 mg/5 ml, susp. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORANEVI1S2", "50 mg/5 ml, oral susp., 240 ml, bot. [fr:50 mg/5 ml, susp. orale, 240 ml, fl.]", Unit.ML),
            new Format("DORANEVI2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORANEVI5TD", "50 mg, disp. tab. [fr:50 mg, comp. disp.]", Unit.TABLET)
        ),
        new Drug("DORANICA", "NICARDIPINE hydrochloride [fr:NICARDIPINE chlorhydrate]").withFormats(
            new Format("DORANICA2TB", "20 mg, breakable tab. [fr:20 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORANICO", "NICOTINAMIDE (vitamin PP) [fr:NICOTINAMIDE (vitamine PP)]").withFormats(
            new Format("DORANICO1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORANIFE", "NIFEDIPINE, 10 mg, immediate release [fr:NIFEDIPINE, 10 mg]").withFormats(
            new Format("DORANIFE1C", "soft caps. [fr:caps.molle lib. immédiate]", Unit.CAPSULE),
            new Format("DORANIFE1TI", "tab. [fr:comp. lib. immédiate]", Unit.TABLET)
        ),
        new Drug("DORANIFU", "NIFURTIMOX [fr:NIFURTIMOX]").withFormats(
            new Format("DORANIFU1T", "120 mg, tab. [fr:120 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORANYST", "NYSTATIN, 100 [fr:NYSTATINE]").withFormats(
            new Format("DORANYST1S", "000 IU/ ml, oral susp. [fr:100.000 UI/ ml, susp. orale]", Unit.MG)
        ),
        new Drug("DORAOLAN", "OLANZAPINE [fr:OLANZAPINE]").withFormats(
            new Format("DORAOLAN2T", "2.5 mg, tab. [fr:2,5 mg, comp.]", Unit.TABLET),
            new Format("DORAOLAN5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAOMEP", "OMEPRAZOLE [fr:OMEPRAZOLE]").withFormats(
            new Format("DORAOMEP1TDG", "10 mg, disp. gastro-resistant tab. [fr:10 mg, comp. disp. gastro-résistant]", Unit.TABLET),
            new Format("DORAOMEP2CG", "20 mg, gastro-resistant caps. [fr:20 mg, gél. gastrorésistante]", Unit.CAPSULE)
        ),
        new Drug("DORAONDA", "ONDANSETRON [fr:ONDANSETRON]").withFormats(
            new Format("DORAONDA1S", "HCl, eq. 4 mg/5 ml base, oral sol., 50 ml, bot. [fr:HCl, éq. 4 mg/5 ml base, sol. orale, 50 ml, fl.]", Unit.ML),
            new Format("DORAONDA4T", "hydrochloride, eq. 4 mg base, tab. [fr:chlorhydrate, éq. 4 mg base, comp.]", Unit.TABLET),
            new Format("DORAONDA8T", "hydrochloride, eq. 8 mg base, tab. [fr:chlorhydrate, éq. 8 mg base, comp]", Unit.TABLET)
        ),
        new Drug("DORAORMA", "RESOMAL, rehydration acute complic. malnut., sach. 84g/2l [fr:RESOMAL, réhydratation malnut. aiguë compliq, sach. 84g/2l]").withFormats(
            new Format("DORAORMA2S8", "", Unit.MG)
        ),
        new Drug("DORAORSA", "ORAL REHYDRATION SALTS (ORS) low osmol., sachet 20.5 g/1l [fr:SELS REHYDRATATION ORALE (SRO) basse osmol. sachet 20]").withFormats(
            new Format("DORAORSA2S", " [fr:5 g/1l]", Unit.MG)
        ),
        new Drug("DORAOSEL", "OSELTAMIVIR phosphate [fr:OSELTAMIVIR phosphate]").withFormats(
            new Format("DORAOSEL7C", "eq. 75 mg base, caps. [fr:éq. 75 mg base, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAPARA", "PARACETAMOL (acetaminophen) [fr:PARACETAMOL (acétaminophène)]").withFormats(
            new Format("DORAPARA1S2", "120 mg/5 ml,oral susp.,100 ml bot. [fr:120 mg/5 ml,susp.orale,100 ml fl.]", Unit.ML),
            new Format("DORAPARA1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET),
            new Format("DORAPARA5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPARX", "PAROXETINE [fr:PAROXETINE]").withFormats(
            new Format("DORAPARX2TB", "20 mg, breakable tab. [fr:20 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORAPASA", "PARA-AMINOSALICYLIC acid [fr:Acide PARA-AMINOSALICYLIQUE]").withFormats(
            new Format("DORAPASA4S", "(PAS), delayed rel.gran, 4 g, sach. [fr:(PAS), gran.lib.ret., 4 g, sach.]", Unit.MG),
            new Format("DORAPASA4S2", "(PAS),del.rel.gran, 4g, sach.(25°C) [fr:(PAS),gran.lib.ret,4g,sach(25°C)]", Unit.MG)
        ),
        new Drug("DORAPASS", "PARA-AMINOSALICYLATE sodium [fr:PARA-AMINOSALICYLATE sodique]").withFormats(
            new Format("DORAPASS1", "del.rel.gran 60%w/w, 100g jar [fr:gran.lib.prol 60%w/w, 100g pot]", Unit.ML),
            new Format("DORAPASS5S", "5.52 g, powder oral sol., sach. [fr:5,52 g, poudre sol.orale, sach]", Unit.MG),
            new Format("DORAPASS9S", "del.rel.gran 60%w/w, 9.2g sach. [fr:gran.lib.prol 60%w/w,9,2g sach]", Unit.ML)
        ),
        new Drug("DORAPEGL", "POLYETHYLENE GLYCOL, powder, sachet [fr:POLYETHYLENE GLYCOL, poudre, sachet]").withFormats(
            new Format("DORAPEGL1P", "", Unit.MG)
        ),
        new Drug("DORAPENV", "PHENOXYMETHYLPENICILLIN").withFormats(
            new Format("DORAPENV1S1", "125 mg/5 ml, powd.oral sol,100 ml, bot [fr:PHENOXYMETHYLPENICILLINE,125 mg/5 ml,poudre sol.orale,100 ml,fl]", Unit.ML),
            new Format("DORAPENV2T", "250 mg, tab. [fr:PHENOXYMETHYLPENICILLINE, 250 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPHEN", "PHENOBARBITAL [fr:PHENOBARBITAL]").withFormats(
            new Format("DORAPHEN1T", "15 mg, tab. [fr:15 mg, comp.]", Unit.TABLET),
            new Format("DORAPHEN3T", "30 mg, tab. [fr:30 mg, comp.]", Unit.TABLET),
            new Format("DORAPHEN5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET),
            new Format("DORAPHEN6T", "60 mg, tab. [fr:60 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPHEY", "").withFormats(
            new Format("DORAPHEY1S", "PHENYTOIN, 30 mg/5 ml, oral susp., 500 ml, bot. [fr:PHENYTOINE, 30 mg/5 ml, susp. orale, 500 ml, fl.]", Unit.ML),
            new Format("DORAPHEY1T", "PHENYTOIN sodium, 100 mg, tab. [fr:PHENYTOINE sodique, 100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPHLO", "PHLOROGLUCINOL [fr:PHLOROGLUCINOL]").withFormats(
            new Format("DORAPHLO8TOD", "80 mg, orodisp. tab. [fr:80 mg, comp. orodisp.]", Unit.TABLET)
        ),
        new Drug("DORAPHYT", "PHYTOMENADIONE (vitamin K1), 10 mg/ ml [fr:PHYTOMENADIONE (vitamine K1), 10 mg/ ml]").withFormats(
            new Format("DORAPHYT1A1", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DORAPOTC", "POTASSIUM chloride [fr:POTASSIUM chlorure]").withFormats(
            new Format("DORAPOTC6TP", "600 mg (8mEq), prolonged-release tab. [fr:600 mg (8mEq), comp. libération prolongée]", Unit.TABLET),
            new Format("DORAPOTC7S", "7.5% w/v, 1mmol K/ ml,oral sol.,500 ml,bot [fr:7.5% p/v, 1mmol K/ ml, sol.orale,500 ml,fl]", Unit.ML)
        ),
        new Drug("DORAPRAZ", "PRAZIQUANTEL [fr:PRAZIQUANTEL]").withFormats(
            new Format("DORAPRAZ6TB", "600 mg, break. tab. [fr:600 mg, comp. séc.]", Unit.TABLET)
        ),
        new Drug("DORAPRED", "").withFormats(
            new Format("DORAPRED2TOD", "PREDNISOLONE 20 mg, orodisp. tablet [fr:PREDNISOLONE 20 mg, comp. orodisp.]", Unit.MG),
            new Format("DORAPRED5T", "PREDNISOLONE, 5 mg, tab. [fr:PREDNISOLONE, 5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPRIM", "PRIMAQUINE diphosphate, eq. [fr:PRIMAQUINE diphosphate, éq.]").withFormats(
            new Format("DORAPRIM1T", "15 mg base, tab. [fr:15 mg base, comp.]", Unit.TABLET),
            new Format("DORAPRIM7T", "7.5 mg base, tab. [fr:7.5 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPRIS", "PRISTINAMYCIN [fr:PRISTINAMYCINE 500 mg, comp. séc.]").withFormats(
            new Format("DORAPRIS5TB", "500 mg, break. tab.", Unit.TABLET)
        ),
        new Drug("DORAPROM", "").withFormats(
            new Format("DORAPROM2T", "PROMETHAZINE hydrochloride, eq. 25 mg base, tab. [fr:PROMETHAZINE chlorhydrate, éq. 25 mg base, comp.]", Unit.TABLET),
            new Format("DORAPROM5S", "PROMETHAZINE, 5 mg/5 ml, syrup, 150 ml, bot. [fr:PROMETHAZINE, 5 mg/5 ml, sirop, 150 ml, fl.]", Unit.ML),
            new Format("DORAPROM5S1", "PROMETHAZINE, 5 mg/5 ml, oral solution, 100 ml, bot [fr:PROMETHAZINE, 5 mg/5 ml, sol. orale, 100 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAPRON", "PROTHIONAMIDE [fr:PROTHIONAMIDE]").withFormats(
            new Format("DORAPRON2T1", "250 mg, tab., blister [fr:250 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORAPYRA", "PYRANTEL, 250 mg/5 ml, oral susp. [fr:PYRANTEL, 250 mg/5 ml, susp. orale]").withFormats(
            new Format("DORAPYRA1S", "15 ml, bot. [fr:15 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAPYRI", "PYRIDOXINE hydrochloride (vitamin B6) [fr:PYRIDOXINE chlorhydrate (vitamine B6)]").withFormats(
            new Format("DORAPYRI1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET),
            new Format("DORAPYRI5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPYRM", "PYRIMETHAMINE [fr:PYRIMETHAMINE]").withFormats(
            new Format("DORAPYRM2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPYRZ", "PYRAZINAMIDE (Z) [fr:PYRAZINAMIDE (Z)]").withFormats(
            new Format("DORAPYRZ1T1", "150 mg, disp. tab., blister [fr:150 mg, comp. disp., blister]", Unit.TABLET),
            new Format("DORAPYRZ1T3", "150 mg, disp. tab., bulk [fr:150 mg, comp. disp., vrac]", Unit.TABLET),
            new Format("DORAPYRZ4T1", "400 mg, tab., blister [fr:400 mg, comp., blister]", Unit.TABLET),
            new Format("DORAPYRZ4T3", "400 mg, tab., bulk [fr:400 mg, comp., vrac]", Unit.TABLET)
        ),
        new Drug("DORAQUIN", "QUININE sulfate [fr:QUININE sulfate]").withFormats(
            new Format("DORAQUIN3T", "300 mg, tab. [fr:300 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORARALT", "RALTEGRAVIR potassium (RAL), eq. [fr:RALTEGRAVIR potassique (RAL), éq.]").withFormats(
            new Format("DORARALT1TC", "100 mg base, chew. tab. [fr:100 mg base, comp.à macher]", Unit.TABLET),
            new Format("DORARALT2TC", "25 mg base, chew. tab. [fr:25 mg base, comp. à mâcher]", Unit.TABLET),
            new Format("DORARALT4T", "400 mg base, tab. [fr:400 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORARAMI", "RAMIPRIL [fr:RAMIPRIL]").withFormats(
            new Format("DORARAMI1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET),
            new Format("DORARAMI2T", "2.5 mg, tab. [fr:2,5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORARANI", "RANITIDINE, 150 mg [fr:RANITIDINE, 150 mg]").withFormats(
            new Format("DORARANI1T", "tab. [fr:comp.]", Unit.TABLET),
            new Format("DORARANI1TE", "effervescent tab. [fr:comp. effervescent]", Unit.TABLET)
        ),
        new Drug("DORARETI", "RETINOL (vitamin A) stabil., 200 [fr:RETINOL (vitamine A) stabilisé]").withFormats(
            new Format("DORARETI2C", "000 IU, soft gelat. caps. [fr:200.000 UI, caps. molle]", Unit.CAPSULE)
        ),
        new Drug("DORARIBA", "RIBAVIRIN [fr:RIBAVIRINE]").withFormats(
            new Format("DORARIBA1S", "200 mg/5 ml, oral sol., 100 ml, bot. [fr:200 mg/5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORARIBA2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE),
            new Format("DORARIBA2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORARIBA4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORARIFA", "RIFAMPICIN (R) [fr:RIFAMPICINE (R)]").withFormats(
            new Format("DORARIFA1C1", "150 mg, caps. blister [fr:150 mg, gél. blister]", Unit.CAPSULE),
            new Format("DORARIFA1C3", "150 mg, caps. bulk [fr:150 mg, gél. vrac]", Unit.CAPSULE),
            new Format("DORARIFA1T1", "150 mg, tab., blister [fr:150 mg, comp., blister]", Unit.TABLET),
            new Format("DORARIFA1T3", "150 mg, tab., bulk [fr:150 mg, comp., vrac]", Unit.TABLET),
            new Format("DORARIFA3C1", "300 mg, caps. blister [fr:300 mg, gél. blister]", Unit.CAPSULE),
            new Format("DORARIFA3C3", "300 mg, caps. bulk [fr:300 mg, gél. vrac]", Unit.CAPSULE)
        ),
        new Drug("DORARIFB", "RIFABUTIN [fr:RIFABUTINE]").withFormats(
            new Format("DORARIFB1C", "150 mg, caps. [fr:150 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORARIFP", "RIFAPENTINE [fr:RIFAPENTINE]").withFormats(
            new Format("DORARIFP1T1", "150 mg, tab., blister [fr:150 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORARISP", "RISPERIDONE [fr:RISPERIDONE]").withFormats(
            new Format("DORARISP1T", "1 mg, tab. [fr:1 mg, comp.]", Unit.TABLET),
            new Format("DORARISP2T", "2 mg, tab. [fr:2 mg, comp.]", Unit.TABLET),
            new Format("DORARISP4T", "4 mg, tab. [fr:4 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORARITO", "RITONAVIR (r) [fr:RITONAVIR (r)]").withFormats(
            new Format("DORARITO1T2", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET),
            new Format("DORARITO2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET),
            new Format("DORARITO8S", "400 mg/5 ml, oral sol., 90 ml, bot. [fr:400 mg/5 ml, sol. orale, 90 ml, fl.]", Unit.ML)
        ),
        new Drug("DORASALB", "").withFormats(
            new Format("DORASALB1N", "SALBUTAMOL, solution for nebulizer, 2 mg/ ml, 2.5 ml monodose [fr:SALBUTAMOL, solution pour nébuliseur, 2 mg/ ml, 2,5 ml unidose]", Unit.MG),
            new Format("DORASALB2SF", "SALBUTAMOL sulfate, eq.0.1 mg base/puff, 200 puffs, aerosol [fr:SALBUTAMOL sulfate, éq.0,1 mg base/bouffée, 200 bouff.aérosol]", Unit.MG)
        ),
        new Drug("DORASALM", "SALMETEROL, 25µg/puff [fr:SALMETEROL, 25µg/bouffée]").withFormats(
            new Format("DORASALM2SF", "120 puffs, aerosol [fr:120 bouffées, aerosol]", Unit.MG)
        ),
        new Drug("DORASERT", "SERTRALINE hydrochloride, eq. [fr:SERTRALINE chlorhydrate, éq.]").withFormats(
            new Format("DORASERT1T", "100 mg base, tab. [fr:100 mg base, comp.]", Unit.TABLET),
            new Format("DORASERT5T", "50 mg base, tab. [fr:50 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORASODC", "SODIUM chloride 6%, for nebulizer [fr:SODIUM chlorure, 6%, pour nébulisation]").withFormats(
            new Format("DORASODC6V", "4 ml, vial [fr:4 ml, fl.]", Unit.ML)
        ),
        new Drug("DORASOFO", "SOFOSBUVIR [fr:SOFOSBUVIR]").withFormats(
            new Format("DORASOFO4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORASOVE", "SOFOSBUVIR (SOF) 400 mg / VELPATASVIR (VEL) 100 mg, tab. [fr:SOFOSBUVIR (SOF) 400 mg / VELPATASVIR (VEL) 100 mg, comp.]").withFormats(
            new Format("DORASOVE41T", "", Unit.TABLET)
        ),
        new Drug("DORASOVV", "SOFOSBUVIR 400 mg/ VELPATASVIR 100 mg/ VOXILAPREVIR 100 mg,tab. [fr:SOFOSBUVIR 400 mg/ VELPATASVIR 100 mg/ VOXILAPREVIR 100 mg,comp]").withFormats(
            new Format("DORASOVV411T", "", Unit.TABLET)
        ),
        new Drug("DORASPAQ", "SP [fr:SP]").withFormats(
            new Format("DORASPAQ1TD2", "1x250/12.5 mg+ AQ 3xeq.75-76.5 mg base,cobl.disp.tb,4.5-8 kg [fr:1x250/12.5 mg+AQ 3x eq.75-76.5 mg base,cobl.cp.disp,4.5-8 kg]", Unit.MG),
            new Format("DORASPAQ2TD2", "1x500/25 mg + AQ 3x eq.150-153 mg base,cobl.disp.tab,9-17 kg [fr:1x500/25 mg+ AQ 3x éq.150-153 mg base,cobl.comp.disp,9-17 kg]", Unit.TABLET)
        ),
        new Drug("DORASPIR", "SPIRONOLACTONE [fr:SPIRONOLACTONE]").withFormats(
            new Format("DORASPIR2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORASUCC", "SUCCIMER [fr:SUCCIMER]").withFormats(
            new Format("DORASUCC2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORASUDI", "SULFADIAZINE [fr:SULFADIAZINE]").withFormats(
            new Format("DORASUDI5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORASULP", "SULFADOXINE, 500 mg / PYRIMETHAMINE [fr:SULFADOXINE, 500 mg / PYRIMETHAMINE]").withFormats(
            new Format("DORASULP5T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATEEF", "TDF 300 mg / FTC 200 mg / EFV 600 mg, tab. [fr:TDF 300 mg / FTC 200 mg / EFV 600 mg, comp.]").withFormats(
            new Format("DORATEEF1T", "", Unit.TABLET)
        ),
        new Drug("DORATEEM", "TDF 300 mg / FTC 200 mg, tab. [fr:TDF 300 mg / FTC 200 mg, comp.]").withFormats(
            new Format("DORATEEM1T", "", Unit.TABLET)
        ),
        new Drug("DORATELA", "TDF 300 mg / 3TC 300 mg, tab. [fr:TDF 300 mg / 3TC 300 mg, comp.]").withFormats(
            new Format("DORATELA1T", "", Unit.TABLET)
        ),
        new Drug("DORATELD", "TDF 300 mg / 3TC 300 mg / DTG 50 mg, tab. [fr:TDF 300 mg / 3TC 300 mg / DTG 50 mg, comp.]").withFormats(
            new Format("DORATELD1T", "", Unit.TABLET)
        ),
        new Drug("DORATELE", "TDF 300 mg / 3TC 300 mg / EFV 600 mg, tab. [fr:TDF 300 mg / 3TC 300 mg / EFV 600 mg, comp.]").withFormats(
            new Format("DORATELE1T", "", Unit.TABLET)
        ),
        new Drug("DORATENO", "TENOFOVIR DISOPROXIL [fr:TENOFOVIR DISOPROXIL]").withFormats(
            new Format("DORATENO2T", "FUMARATE, eq. 163 mg base, tab. [fr:FUMARATE, éq. 163 mg base, comp.]", Unit.TABLET),
            new Format("DORATENO3T", "fumarate 300 mg, eq. 245 mg base, tab. [fr:fumarate 300 mg, éq. 245 mg base, comp]", Unit.TABLET)
        ),
        new Drug("DORATHIA", "THIAMINE hydrochloride (vitamin B1) [fr:THIAMINE chlorhydrate (vitamine B1)]").withFormats(
            new Format("DORATHIA2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORATHIA5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATINI", "TINIDAZOLE [fr:TINIDAZOLE]").withFormats(
            new Format("DORATINI5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATRAM", "TRAMADOL hydrochloride [fr:TRAMADOL chlorhydrate]").withFormats(
            new Format("DORATRAM1S", "100 mg/ ml/40 drops, 10 ml, bot. [fr:100 mg/ ml/40 gouttes, 10 ml, fl.]", Unit.MG),
            new Format("DORATRAM5C", "50 mg, caps. [fr:50 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORATRAN", "TRANEXAMIC ACID [fr:ACIDE TRANEXAMIQUE]").withFormats(
            new Format("DORATRAN5T", "500 mg tab [fr:500 mg comp]", Unit.TABLET)
        ),
        new Drug("DORATRIB", "TRICLABENDAZOLE [fr:TRICLABENDAZOLE]").withFormats(
            new Format("DORATRIB2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATRIH", "TRIHEXYPHENIDYL hydrochloride [fr:TRIHEXYPHENIDYLE chlorhydrate]").withFormats(
            new Format("DORATRIH2T", "2 mg, tab. [fr:2 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAULIP", "ULIPRISTAL acetate [fr:ULIPRISTAL acétate]").withFormats(
            new Format("DORAULIP3T", "30 mg, tab. [fr:30 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAVALG", "VALGANCICLOVIR hydrochloride [fr:VALGANCICLOVIR chlorhydrate]").withFormats(
            new Format("DORAVALG4T", "eq. 450 mg base, tab. [fr:éq. 450 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAVALP", "VALPROATE SODIUM [fr:VALPROATE de]").withFormats(
            new Format("DORAVALP1S", "200 mg/ ml, 40 ml, bot. + syringe [fr:SODIUM 200 mg/ ml, 40 ml, fl. + seringue]", Unit.MG),
            new Format("DORAVALP2S", "200 mg/5 ml, 300 ml, bot. [fr:SODIUM 200 mg/5 ml, 300 ml, fl.]", Unit.ML),
            new Format("DORAVALP2TG", "200 mg, gastro-resistant tab. [fr:SODIUM, 200 mg, comp. gastro-résistant]", Unit.TABLET),
            new Format("DORAVALP5TG", "500 mg, gastro-resistant tab. [fr:SODIUM, 500 mg, comp. gastro-résistant]", Unit.TABLET)
        ),
        new Drug("DORAVERA", "VERAPAMIL hydrochloride [fr:VERAPAMIL chlorhydrate]").withFormats(
            new Format("DORAVERA4T", "40 mg, tab. [fr:40 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAVITB", "VITAMINE B COMPLEX, tab. [fr:VITAMINE B COMPLEX, comp.]").withFormats(
            new Format("DORAVITB1T", "", Unit.TABLET)
        ),
        new Drug("DORAWARF", "WARFARIN [fr:WARFARINE]").withFormats(
            new Format("DORAWARF1T", "1 mg, tab. [fr:1 mg, comp.]", Unit.TABLET),
            new Format("DORAWARF5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAYIDO", "ZIDOVUDINE (AZT) [fr:ZIDOVUDINE (AZT)]").withFormats(
            new Format("DORAYIDO1S", "50 mg/5 ml, oral sol., 100 ml bot. [fr:50 mg/5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORAYIDO2S", "50 mg/5 ml, oral sol., 200 ml, bot. [fr:50 mg/5 ml, sol. orale, 200 ml, fl.]", Unit.ML),
            new Format("DORAYIDO3S", "50 mg/5 ml, oral sol., 240 ml, bot. [fr:50 mg/5 ml, sol. orale, 240 ml, fl.]", Unit.ML),
            new Format("DORAYIDO3T", "300 mg, tab. [fr:300 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAYILA", "AZT [fr:AZT]").withFormats(
            new Format("DORAYILA1T", "60 mg / 3TC 30 mg, tab. [fr:60 mg / 3TC 30 mg , comp.]", Unit.TABLET),
            new Format("DORAYILA1TD", "60 mg / 3TC 30 mg , disp. tab. [fr:60 mg / 3TC 30 mg, comp. disp.]", Unit.TABLET),
            new Format("DORAYILA2T", "300 mg / 3TC 150 mg, tab. [fr:300 mg / 3TC 150 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAYILE", "AZT 300 mg / 3TC 150 mg [fr:AZT 300 mg / 3TC 150 mg]").withFormats(
            new Format("DORAYILE1T2", "x 2 + EFV 600 mg x 1,coblister of tab [fr:x 2 + EFV 600 mg x 1, coblister comp]", Unit.TABLET),
            new Format("DORAYILE1T4", "x60 + EFV 600 mg x30, co-pack of tab. [fr:x60 + EFV 600 mg x30, co-pack comp.]", Unit.TABLET)
        ),
        new Drug("DORAYILN", "AZT [fr:AZT]").withFormats(
            new Format("DORAYILN1TD", "60 mg / 3TC 30 mg / NVP 50 mg, dispersible tab. [fr:60 mg / 3TC 30 mg / NVP 50 mg, comp. dispersible]", Unit.TABLET),
            new Format("DORAYILN2T", "300 mg / 3TC 150 mg / NVP 200 mg, tab. [fr:300 mg / 3TC 150 mg / NVP 200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAYINS", "ZINC sulfate, eq. to 20 mg zinc mineral, dispersible tab. [fr:ZINC sulfate, éq. à 20 mg de zinc minéral, comp. dispers.]").withFormats(
            new Format("DORAYINS2T", "", Unit.TABLET)
        )
    );

    Category EXTERNAL = new Category("DEXT", "external", DosingType.QUANTITY).withDrugs(
        new Drug("DEXOACIV", "ACICLOVIR, 3%, eye ointment, sterile [fr:ACICLOVIR, 3%, pommade ophtalmique, stérile, 4]").withFormats(
            new Format("DEXOACIV3T4", "4.5 g, tube [fr:5 g, tube]", Unit.ML)
        ),
        new Drug("DEXOATRO", "ATROPINE sulfate, 1%, eye drops, ster [fr:ATROPINE sulfate, 1%, collyre, stér, 0]").withFormats(
            new Format("DEXOATRO1D4", "0.4 ml, unidose, amp. [fr:4 ml, unidose, amp.]", Unit.ML)
        ),
        new Drug("DEXOCHLO", "CHLORAMPHENICOL, 0.5%, eye drops, sterile [fr:CHLORAMPHENICOL, 0,5%, collyre, stérile]").withFormats(
            new Format("DEXOCHLO5D1", "10 ml, bot. [fr:10 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXODENP", "DEXAMET.5 mg/NEOMYC.17500IU/POLYMYXIN B 30000IU,eye drops [fr:DEXAMET.5 mg/NEOMYC.17500UI/POLYMYXINE B 30000UI, collyre]").withFormats(
            new Format("DEXODENP513D5", "5 ml [fr:5 ml]", Unit.MG)
        ),
        new Drug("DEXODEXN", "DEXAMETHASONE 5 mg / NEOMYCINE 17500 IU, eye drops [fr:DEXAMETHASONE 5 mg / NEOMYCINE 17500 UI, collyre]").withFormats(
            new Format("DEXODEXN5D5", "5 ml, bot [fr:5 ml, fl.]", Unit.MG)
        ),
        new Drug("DEXODEXT", "DEXAMETHASONE 5 mg / TOBRAMYCIN 15 mg, eye drops [fr:DEXAMETHASONE 5 mg / TOBRAMYCINE 15 mg, collyre]").withFormats(
            new Format("DEXODEXT51D", "5 ml, bot [fr:5 ml, fl.]", Unit.MG)
        ),
        new Drug("DEXOFLUO", "FLUORESCEIN [fr:FLUORESCEINE]").withFormats(
            new Format("DEXOFLUO1D4", "0.5%, eye drops, ster, 0.4 ml, unidose, amp. [fr:0,5%, collyre, stérile, 0,4 ml, unidose, amp.]", Unit.ML),
            new Format("DEXOFLUO2D5", "2 %, eye drops, sterile, 0.5 ml, unidose, amp [fr:2 %, collyre, stérile, 0,5 ml, unidose, amp.]", Unit.MG)
        ),
        new Drug("DEXOGANC", "GANCICLOVIR [fr:GANCICLOVIR]").withFormats(
            new Format("DEXOGANC1G", "0.15%, eye gel, sterile [fr:0.15%, gel ophtalmique, stérile]", Unit.ML)
        ),
        new Drug("DEXOOXYB", "OXYBUPROCAINE, 0.4 %, eye drops, ster [fr:OXYBUPROCAINE, 0,4 %, collyre, stér, 0]").withFormats(
            new Format("DEXOOXYB1", "0.5 ml, unidose, amp. [fr:5 ml, unidose, amp.]", Unit.MG)
        ),
        new Drug("DEXOPHEE", "PHENYLEPHRINE hydrochloride [fr:PHENYLEPHRINE chlorhydrate]").withFormats(
            new Format("DEXOPHEE5D", "5%, eye drops [fr:5%, collyre]", Unit.ML)
        ),
        new Drug("DEXOPILO", "PILOCARPINE hydrochloride, 2%, eye drops, sterile [fr:PILOCARPINE chlorhydrate, 2%, collyre, stérile]").withFormats(
            new Format("DEXOPILO2D1", "10 ml, bot [fr:10 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXORIFM", "").withFormats(
            new Format("DEXORIFM1D1", "RIFAMYCINE sodium, 1 000,000lU/100 ml, eye drops, 10 ml, bot. [fr:RIFAMYCINE sodique, 1 000 000Ul/100 ml, collyre, 10 ml, fl.]", Unit.ML),
            new Format("DEXORIFM1O5", "RIFAMYCINE, 1000,000UI/100g, eye ointment, sterile, 5g, tube [fr:RIFAMYCINE, 1000 000UI/100g, pom. ophtalm, stérile, 5g, tube]", Unit.MG)
        ),
        new Drug("DEXOSODC", "SODIUM CHLORIDE 0.9%, eye drops, sterile [fr:SERUM PHYSIOLOGIQUE, chlorure de sodium 0,9 %, stérile]").withFormats(
            new Format("DEXOSODC9D5", "5 ml [fr:5 ml]", Unit.ML)
        ),
        new Drug("DEXOTETR", "TETRACYCLINE hydrochloride, 1%, eye ointment, ster [fr:TETRACYCLINE chlorhydrate, 1%, pommade opht., stér]").withFormats(
            new Format("DEXOTETR1O5", "5g, tube [fr:5g, tube]", Unit.ML)
        ),
        new Drug("DEXOTROP", "").withFormats(
            new Format("DEXOTROP1D0", "TROPICAMIDE 1% eye drops 0.5 ml, unidose, amp [fr:TROPICAMIDE 1% collyre 0.5 ml, unidose, amp.]", Unit.ML),
            new Format("DEXOTROP5D4", "TROPICAMIDE, 0.5%, eye drops, sterile, 0.4 ml, unidose, amp. [fr:TROPICAMIDE, 0,5%, collyre, stérile, 0,4 ml, unidose, amp.]", Unit.ML)
        ),
        new Drug("DEXTACIV", "ACICLOVIR, 5%, cream [fr:ACICLOVIR, 5%, crème]").withFormats(
            new Format("DEXTACIV5C1", "10 g, tube [fr:10 g, tube]", Unit.ML)
        ),
        new Drug("DEXTALCD", "DENATURED ALCOHOL [fr:ALCOOL DENATURE]").withFormats(
            new Format("DEXTALCDB5", "500 ml, bot. [fr:500 ml, fl.]", Unit.MG)
        ),
        new Drug("DEXTALCO", "ALCOHOL-BASED HAND RUB [fr:HYDRO-ALCOOLIQUE]").withFormats(
            new Format("DEXTALCO1G", "gel, 100 ml, bot. [fr:gel, 100 ml, fl.]", Unit.MG),
            new Format("DEXTALCO3G", "gel, 30 ml, bot. [fr:gel, 30 ml, fl.]", Unit.MG),
            new Format("DEXTALCO5S", "solution, 500 ml, bot. [fr:solution, 500 ml, fl.]", Unit.MG)
        ),
        new Drug("DEXTANTH", "ANTIHAEMORROID [fr:ANTI HEMORROIDAIRE]").withFormats(
            new Format("DEXTANTH1C2", "cream, 25 g, tube [fr:crème, 25 g, tube]", Unit.MG),
            new Format("DEXTANTH1O2", "ointment, 25 g, tube [fr:pommade, 25 g, tube]", Unit.MG)
        ),
        new Drug("DEXTARTS", "ARTESUNATE [fr:ARTESUNATE]").withFormats(
            new Format("DEXTARTS1RC", "100 mg, rectal caps. [fr:100 mg, caps. rectale]", Unit.CAPSULE)
        ),
        new Drug("DEXTBENS", "BENZOIC ACID 6% / SALICYLIC ACID 3%, ointment [fr:ACIDE BENZOIQUE 6% / ACIDE SALICYLIQUE 3%, pom.]").withFormats(
            new Format("DEXTBENS6O4", "40 g, tube [fr:40 g, tube]", Unit.ML)
        ),
        new Drug("DEXTBENZ", "BENZYL BENZOATE, 25%, lotion [fr:BENZOATE DE BENZYLE, 25%, lotion]").withFormats(
            new Format("DEXTBENZ2L1", "1 l, bot. [fr:1 l, fl.]", Unit.ML)
        ),
        new Drug("DEXTBETM", "BETAMETHASONE dipropionate, eq.0.05% base, cream [fr:BETAMETHASONE dipropionate, éq.0.05% base, crème]").withFormats(
            new Format("DEXTBETM5C3", "30 g, tube [fr:30 g, tube]", Unit.ML)
        ),
        new Drug("DEXTCALA", "CALAMINE, 15%, lotion [fr:CALAMINE, 15%, lotion]").withFormats(
            new Format("DEXTCALA1L5", "500 ml, bot. [fr:500 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTCHLH", "CHLORHEXIDINE [fr:CHLORHEXIDINE]").withFormats(
            new Format("DEXTCHLH2AS", "digluconate 2%, aqueous solution, 100 ml, bot. [fr:gluconate 2%, solution aqueuse, 100 ml fl.]", Unit.ML),
            new Format("DEXTCHLH2S", "digluconate 0.2%, mouthwash, sol., 300 ml, bot. [fr:digluconate 0,2%, bain de bouche,sol.,300 ml,fl]", Unit.ML),
            new Format("DEXTCHLH2S5", "0,2%, aqueous solution, 5 ml, unidose [fr:0,2%, solution aqueuse, 5 ml, unidose]", Unit.ML),
            new Format("DEXTCHLH2SA2", "2%, alcohol solution, 250 ml, bot. [fr:2%, solution alcoolique, 250 ml, fl.]", Unit.ML),
            new Format("DEXTCHLH5S1", "digluconate 5%, solution, 1 l, bot. [fr:digluconate 5%, solution, 1 l, fl.]", Unit.ML),
            new Format("DEXTCHLH5S9", "digluc.0.5 ml/0.5g/100 ml, mouthwash, sol, 90 ml [fr:digluc.0.5 ml/0.5g/100 ml,bain d.bouche,sol,90 ml]", Unit.ML),
            new Format("DEXTCHLH7G2", "digluconate 7.1%, gel, 20g tube [fr:digluconate 7,1%, gel, 20g tube]", Unit.ML),
            new Format("DEXTCHLH7G3", "digluconate 7.1%, gel, 3g sachet [fr:digluconate 7,1%, gel, 3g sachet]", Unit.ML),
            new Format("DEXTCHLHA2S2", "2%, 70 % isopropyl alcohol, sol., 250 ml, bot. [fr:2%, 70 % d'alcool isopropylique,sol.,250 ml,fl.]", Unit.ML),
            new Format("DEXTCHLHA2W", "2%, 70 % isopropyl alcohol, WIPE [fr:2%, 70 % d'alcool isopropylique, LINGETTE]", Unit.ML),
            new Format("DEXTCHLHSP4", "digluconate 4%, soap, 500 ml, bot. [fr:digluconate 4%, savon, 500 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTCIPR", "CIPROFLOXACIN [fr:CIPROFLOXACINE, 0]").withFormats(
            new Format("DEXTCIPR1D", "0.3%, ear/eye drops, sterile, bot. [fr:3%, gttes auriculaires/collyre, stérile,fl]", Unit.ML)
        ),
        new Drug("DEXTCLOT", "CLOTRIMAZOLE [fr:CLOTRIMAZOLE]").withFormats(
            new Format("DEXTCLOT1C2", "1%, cream, 20g, tube [fr:1%, crème, 20g, tube]", Unit.ML),
            new Format("DEXTCLOT5T", "500 mg, vaginal tab. + applicator [fr:500 mg, comp. vaginal + applicateur]", Unit.TABLET)
        ),
        new Drug("DEXTCOLD", "COLD CREAM, cream [fr:COLD CREAM, crème]").withFormats(
            new Format("DEXTCOLD1C", "1000 ml, jar [fr:1000 ml, pot]", Unit.MG)
        ),
        new Drug("DEXTDEET", "D.E.E.T., anti-mosquito repellent lotion [fr:D.E.E.T., lotion répulsive anti-moustique]").withFormats(
            new Format("DEXTDEET1C", "30% [fr:30%]", Unit.ML)
        ),
        new Drug("DEXTDIAZ", "DIAZEPAM [fr:DIAZEPAM]").withFormats(
            new Format("DEXTDIAZ1RS", "4 mg/ ml, rectal sol., 2.5 ml, tube [fr:4 mg/ ml, sol. rectale, 2,5 ml, tube]", Unit.MG),
            new Format("DEXTDIAZ2RS", "2 mg/1 ml, rectal sol., 1.25 ml, tube [fr:2 mg/1 ml, sol. rectale, 1,25 ml, tube]", Unit.ML)
        ),
        new Drug("DEXTDICL", "DICLOFENAC 1%, gel [fr:DICLOFENAC 1%, gel]").withFormats(
            new Format("DEXTDICL1G5", "50 g, tube [fr:50 g, tube]", Unit.ML)
        ),
        new Drug("DEXTDINO", "DINOPROSTONE [fr:DINOPROSTONE]").withFormats(
            new Format("DEXTDINO1G", "1 mg, vaginal gel, sterile [fr:1 mg, gel vaginal stérile]", Unit.MG)
        ),
        new Drug("DEXTENEM", "ENEMA, rectal sol. [fr:LAVEMENT, sol. rectale]").withFormats(
            new Format("DEXTENEM5RS", "5 ml, tube [fr:5 ml, tube]", Unit.MG)
        ),
        new Drug("DEXTFENT", "FENTANYL [fr:FENTANYL]").withFormats(
            new Format("DEXTFENT2TP", "2.1 mg/5.25cm2 , 12 μg/h, transdermal patch [fr:2,1 mg/5.25cm2 , 12 μg/h, dispositif transdermique]", Unit.MG),
            new Format("DEXTFENT4TP", "4.2 mg, 25 μg/h, transdermal patch [fr:4,2 mg, 25 μg/h, dispositif transdermique]", Unit.MG)
        ),
        new Drug("DEXTFUSI", "FUSIDIC ACID, 2%, cream [fr:ACIDE FUSIDIQUE, 2%, crème]").withFormats(
            new Format("DEXTFUSI2C3", "30 g, tube [fr:30 g, tube]", Unit.ML)
        ),
        new Drug("DEXTGLYP", "GLYCEROL 15% / PARAFFIN 10%, cream [fr:GLYCEROL 15% / PARAFFINE 10%, crème]").withFormats(
            new Format("DEXTGLYP2C", "250 g, tube [fr:250 g, tube]", Unit.ML)
        ),
        new Drug("DEXTHYDR", "HYDROCORTISONE (acetate or base), 1% [fr:HYDROCORTISONE (acétate ou base), 1%]").withFormats(
            new Format("DEXTHYDR1C1", "cream, 15 g, tube [fr:crème, 15 g, tube]", Unit.ML),
            new Format("DEXTHYDR1O1", "ointment, 15 g, tube [fr:pommade, 15 g, tube]", Unit.ML)
        ),
        new Drug("DEXTHYPE", "HYDROGEN PEROXIDE, 3%, sol. [fr:PEROXYDE D'HYDROGÈNE, 3%, sol.]").withFormats(
            new Format("DEXTHYPE3B2", "250 ml, bot. [fr:250 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTHYSU", "HYALURONATE sodium / SILVER SULFADIAZINE, cream [fr:HYALURONATE de sodium/SULFADIAZINE argent., crème]").withFormats(
            new Format("DEXTHYSU1C", "100g, tube [fr:100g,tube]", Unit.MG)
        ),
        new Drug("DEXTIODP", "POLYVIDONE IODINE [fr:POLYVIDONE IODEE]").withFormats(
            new Format("DEXTIODP1G3", "10%, gel, tube of 30 g [fr:10%, gel, tube de 30 g]", Unit.ML),
            new Format("DEXTIODP1S2", "10%, solution, 200 ml, dropper bot. [fr:10%, solution, 200 ml, fl. verseur]", Unit.ML),
            new Format("DEXTIODPS4", "surgical scrub, 4 %, 125 ml, bot. [fr:savon germicide, 4%, 125 ml, fl.]", Unit.MG),
            new Format("DEXTIODPS75", "surgical scrub, 7.5 %, 500 ml, bot. [fr:savon germicide, 7,5%, 500 ml, fl.]", Unit.MG)
        ),
        new Drug("DEXTIUDE", "INTRA UTERINE DEVICE, LEVONORGESTREL [fr:DISPOSITIF INTRA UTERIN, LEVONORGESTREL]").withFormats(
            new Format("DEXTIUDE1L", "52 mg (LNG-IUD 52) [fr:52 mg (LNG-DIU 52)]", Unit.MG)
        ),
        new Drug("DEXTLICH", "LIDOCAINE 2%/CHLORHEX. digluc.0.25%, jelly [fr:LIDOCAINE 2%/CHLORHEX. digluc.0,25%, gel]").withFormats(
            new Format("DEXTLICH2J", "11 ml, ster, syr. [fr:11 ml, stér, ser.]", Unit.ML)
        ),
        new Drug("DEXTLIDO", "LIDOCAINE [fr:LIDOCAINE]").withFormats(
            new Format("DEXTLIDO2J3", "2%, jelly, sterile, tube [fr:2%, gel, stérile, tube]", Unit.ML)
        ),
        new Drug("DEXTLIDP", "LIDOCAINE 2.5% / PRILOCAINE 2.5%, cream [fr:LIDOCAINE 2,5% / PRILOCAINE 2,5%, crème]").withFormats(
            new Format("DEXTLIDP2C5", "5 g, tube [fr:5 g, tube]", Unit.ML)
        ),
        new Drug("DEXTMAFE", "MAFENIDE acetate, cream [fr:MAFENIDE acetate, crème, 453]").withFormats(
            new Format("DEXTMAFE4C", "453.6 g, jar [fr:6 g, pot]", Unit.MG)
        ),
        new Drug("DEXTMALA", "MALATHION [fr:MALATHION]").withFormats(
            new Format("DEXTMALA5L", "500 mg/100 ml, lotion, bot. [fr:500 mg/100 ml, lotion, fl.]", Unit.ML)
        ),
        new Drug("DEXTMICO", "MICONAZOLE nitrate, 2%, cream [fr:MICONAZOLE nitrate, 2%, crème]").withFormats(
            new Format("DEXTMICO2C3", "30 g, tube [fr:30 g, tube]", Unit.ML)
        ),
        new Drug("DEXTMOSQ", "ANTIPRURITIC CREAM, after mosquito bites, tube [fr:CREME ANTIPRURIGINEUSE, après piqûres de moustiques, tube]").withFormats(
            new Format("DEXTMOSQ1C", "", Unit.MG)
        ),
        new Drug("DEXTMUPI", "MUPIROCIN 2%, ointment [fr:MUPIROCINE 2%, pommade]").withFormats(
            new Format("DEXTMUPI2O1", "15 g, tube [fr:15 g, tube]", Unit.ML)
        ),
        new Drug("DEXTOFLO", "OFLOXACIN, 3 mg/ ml, ear sol. [fr:OFLOXACINE, 3 mg/ ml, sol. auriculaire, 0]").withFormats(
            new Format("DEXTOFLO1S5", "0.5 ml, monodose [fr:5 ml, unidose]", Unit.MG)
        ),
        new Drug("DEXTPARA", "PARACETAMOL (acetaminophen) [fr:PARACETAMOL (acétaminophène)]").withFormats(
            new Format("DEXTPARA12SU", "120 mg, suppository [fr:120 mg, suppositoire]", Unit.MG),
            new Format("DEXTPARA12SU1", "125 mg, suppository [fr:125 mg, suppositoire]", Unit.MG),
            new Format("DEXTPARA2SU", "240 mg, suppository [fr:240 mg, suppositoire]", Unit.MG),
            new Format("DEXTPARA5SU", "500 mg, suppository [fr:500 mg, suppositoire]", Unit.MG)
        ),
        new Drug("DEXTPERM", "PERMETHRIN [fr:PERMETHRINE]").withFormats(
            new Format("DEXTPERM1L1", "1 %, lotion, bot. [fr:1 %, lotion, fl.]", Unit.MG),
            new Format("DEXTPERM5T", "5%, cream, tube [fr:5% crème, tube]", Unit.ML)
        ),
        new Drug("DEXTPHEL", "PHENASONE 4% / LIDOCAINE HCl 1%, ear drops [fr:PHENASONE 4% / LIDOCAINE HCl 1%,gttes auric]").withFormats(
            new Format("DEXTPHEL41D1", "15 ml, bot. [fr:15 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTPODO", "PODOPHYLLOTOXIN, 0.5%, solution [fr:PODOPHYLLOTOXINE, 0,5%, solution, 3]").withFormats(
            new Format("DEXTPODO5S3", "3.5 ml, + 30 applicator tips [fr:5 ml, + 30 applicateurs]", Unit.ML)
        ),
        new Drug("DEXTSILN", "SILVER NITRATE [fr:NITRATE D'ARGENT]").withFormats(
            new Format("DEXTSILN1U", "40%, pencil [fr:40%, crayon]", Unit.ML)
        ),
        new Drug("DEXTSUCE", "SILVER SULFADIAZINE 5g / CERIUM nitrate 11g, cream [fr:SULFADIAZINE ARGENTIQUE 5g / CERIUM nitrate 11g, crème]").withFormats(
            new Format("DEXTSUCE51C", "500g,pot [fr:500g]", Unit.MG)
        ),
        new Drug("DEXTSULZ", "SULFADIAZINE SILVER, 1%, cream, sterile [fr:SULFADIAZINE ARGENTIQUE, 1%, crème, stérile]").withFormats(
            new Format("DEXTSULZ1C5", "50 g, tube [fr:50 g, tube]", Unit.ML),
            new Format("DEXTSULZ1CJ", "500 g, jar [fr:500 g, pot]", Unit.ML)
        ),
        new Drug("DEXTYINO", "ZINC OXIDE [fr:OXYDE DE ZINC]").withFormats(
            new Format("DEXTYINO15O1", "15%, ointment, 100 g, jar [fr:15%, pommade, 100 g, pot]", Unit.ML),
            new Format("DEXTYINO1O1", "10%, ointment, 100 g, tube [fr:10%, pommade, 100 g, tube]", Unit.ML)
        )
    );

    Category INFUSIBLE = new Category("DINF", "infusible", DosingType.QUANTITY_OVER_DURATION).withDrugs(
        new Drug("DINFDERI", "DEXTROSE 5%/ RINGER LACTATE [fr:GLUCOSE 5%/ RINGER LACTATE]").withFormats(
            new Format("DINFDERI5FBF5", "500 ml, flex. bag, PVC free [fr:500 ml, poche souple, sans PVC]", Unit.ML)
        ),
        new Drug("DINFDEXT", "DEXTROSE (GLUCOSE) [fr:GLUCOSE]").withFormats(
            new Format("DINFDEXT1FBF2", "10%, 250 ml, flex. bag, PVC free [fr:10%, 250 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT1FBF5", "10%, 500 ml, flex. bag, PVC free [fr:10%, 500 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT1FBP5", "10%, 500 ml, flex. bag, PVC [fr:10%, 500 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFDEXT1SRF2", "10%, 250 ml, semi-rigid bot., PVC free [fr:10%, 250 ml, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFDEXT1SRF5", "10%, 500 ml, semi-rigid bot., PVC free [fr:10%, 500 ml, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFDEXT5FBF1", "5%, 1 l, flex. bag, PVC free [fr:5%, 1 l, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT5FBF2", "5%, 250 ml, flex. bag, PVC free [fr:5%, 250 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT5FBF5", "5%, 500 ml, flex. bag, PVC free [fr:5%, 500 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT5FBP1", "5%, 1 l, flex. bag, PVC [fr:5%, 1 l, poche souple, PVC]", Unit.ML),
            new Format("DINFDEXT5FBP5", "5%, 500 ml, flex. bag, PVC [fr:5%, 500 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFDEXT5SRF1", "5%, 1 l, semi-rigid bot., PVC free [fr:5%, 1 l, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFDEXT5SRF5", "5%, 500 ml, semi-rigid bot., PVC free [fr:5%, 500 ml, fl. semi-rigide, sans PVC]", Unit.ML)
        ),
        new Drug("DINFMANN", "MANNITOL [fr:MANNITOL]").withFormats(
            new Format("DINFMANN2B5", "20%, 500 ml, bot. [fr:20%, 500 ml, fl.]", Unit.ML),
            new Format("DINFMANN2FBF5", "20 %, 500 ml, flex. bag, PVC free [fr:20 %, 500 ml, poche souple, sans PVC]", Unit.MG)
        ),
        new Drug("DINFPLAS", "MODIFIED FLUID [fr:PLASMA SUBSTITUT, gélatine, 500 ml]").withFormats(
            new Format("DINFPLAS1FBF5", "GELATIN/POLYGELIN, 500 ml, flex.bag, PVC free [fr:poche souple, ss PVC]", Unit.MG),
            new Format("DINFPLAS1FBP5", "GELATIN/POLYGELIN, 500 ml, flex.bag, PVC [fr:poche souple, PVC]", Unit.MG),
            new Format("DINFPLAS1SRF5", "GELATIN/POLYGELIN,500 ml,semi-rigid bt,PVCfree [fr:fl. semi-rigide, ss PVC]", Unit.MG)
        ),
        new Drug("DINFRINL", "RINGER lactate [fr:RINGER lactate]").withFormats(
            new Format("DINFRINL1FBF1", "1 l, flex. bag, PVC free [fr:1 l, poche souple, sans PVC]", Unit.MG),
            new Format("DINFRINL1FBF5", "500 ml, flex. bag, PVC free [fr:500 m l, poche souple, sans PVC]", Unit.MG),
            new Format("DINFRINL1FBP1", "1 l, flex. bag, PVC [fr:1 l, poche souple, PVC]", Unit.MG),
            new Format("DINFRINL1FBP5", "500 ml, flex. bag, PVC [fr:500 m l, poche souple, PVC]", Unit.MG),
            new Format("DINFRINL1SRF1", "1 l, semi-rigid bot., PVC free [fr:1 l, fl. semi-rigide, sans PVC]", Unit.MG),
            new Format("DINFRINL1SRF5", "500 ml, semi-rigid bot., PVC free [fr:500 ml, fl. semi-rigide, sans PVC]", Unit.MG)
        ),
        new Drug("DINFSODC", "SODIUM chloride, 0.9% [fr:SODIUM chlorure, 0,9%]").withFormats(
            new Format("DINFSODC9FBF0", "100 ml, flex. bag, PVC free [fr:100 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFSODC9FBF1", "1 l, flex. bag, PVC free [fr:1 l, poche souple, sans PVC]", Unit.ML),
            new Format("DINFSODC9FBF2", "250 ml, flex. bag, PVC free [fr:250 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFSODC9FBF5", "500 ml, flex. bag, PVC free [fr:500 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFSODC9FBP0", "100 ml, flex. bag, PVC [fr:100 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFSODC9FBP1", "1 l, flex. bag, PVC [fr:1 l, poche souple, PVC]", Unit.ML),
            new Format("DINFSODC9FBP2", "250 ml, flex. bag, PVC [fr:250 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFSODC9FBP5", "500 ml, flex. bag, PVC [fr:500 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFSODC9SRF0", "100 ml, semi-rigid bot., PVC free [fr:100 ml, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFSODC9SRF1", "1 l, semi-rigid bot., PVC free [fr:1 l, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFSODC9SRF2", "250 ml, semi-rigid bot., PVC free [fr:250 ml, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFSODC9SRF5", "500 ml, semi-rigid bot., PVC free [fr:500 ml, fl. semi-rigide, sans PVC]", Unit.ML)
        ),
        new Drug("DINFWATE", "WATER FOR INJECTION [fr:EAU POUR PREPARATION INJECTABLE]").withFormats(
            new Format("DINFWATE1B1", "100 ml, bot. [fr:100 ml, fl.]", Unit.MG),
            new Format("DINFWATE1FB05", "50 ml, flex.bag, PVC free [fr:50 ml, poche souple, ss PVC]", Unit.MG),
            new Format("DINFWATE1FBF1", "100 ml, flex.bag, PVC free [fr:100 ml, poche souple, ss PVC]", Unit.MG),
            new Format("DINFWATE1FBF2", "250 ml, flex.bag, PVC free [fr:250 ml, poche souple, ss PVC]", Unit.MG),
            new Format("DINFWATE1FBP1", "100 ml, flex.bag, PVC [fr:100 ml, poche souple, PVC]", Unit.MG),
            new Format("DINFWATE1FBP2", "250 ml, flex.bag, PVC [fr:250 ml, poche souple, PVC]", Unit.MG),
            new Format("DINFWATE1SRF1", "100 ml, semi-rigid bot., PVC free [fr:100 ml,fl.semi-rigide,ss PVC]", Unit.MG),
            new Format("DINFWATE1SRF2", "250 ml, semi-rigid bot., PVC free [fr:250 ml,fl.semi-rigide,ss PVC]", Unit.MG)
        )
    );

    Category INJECTABLE = new Category("DINJ", "injectable", DosingType.QUANTITY, IV, SC, IM).withDrugs(
        new Drug("DINJACCY", "ACETYLCYSTEINE, 200 mg/ ml [fr:ACETYLCYSTEINE, 200 mg/ ml]").withFormats(
            new Format("DINJACCY2A", "10 ml, amp. [fr:10 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJACIV", "ACICLOVIR sodium, eq. [fr:ACICLOVIR sodique, éq.]").withFormats(
            new Format("DINJACIV2V", "250 mg base, powder, vial [fr:250 mg base, poudre, fl]", Unit.MG),
            new Format("DINJACIV2V1", "25 mg/ ml base, 10 ml, vial [fr:25 mg/ ml base, 10 ml, fl.]", Unit.MG)
        ),
        new Drug("DINJADEN", "ADENOSINE, 3 mg/ ml [fr:ADENOSINE, 3 mg/ ml]").withFormats(
            new Format("DINJADEN6V", "2 ml, vial [fr:2 ml, fl.]", Unit.MG)
        ),
        new Drug("DINJAMBC", "AMPHOTERICIN B conventional [fr:AMPHOTERICINE B conventionnelle]").withFormats(
            new Format("DINJAMBC5V", "50 mg, powder, vial [fr:50 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJAMBL", "AMPHOTERICIN B liposomal complex [fr:AMPHOTERICINE B complexe liposomal]").withFormats(
            new Format("DINJAMBL5V", "50 mg,powder, vial [fr:50 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJAMIK", "AMIKACIN sulfate, eq. 250 mg/ ml base, 2 ml [fr:AMIKACINE sulfate, éq. 250 mg/ ml base, 2 ml]").withFormats(
            new Format("DINJAMIK5A", "amp. [fr:amp.]", Unit.MG),
            new Format("DINJAMIK5V1", "vial [fr:fl.]", Unit.MG)
        ),
        new Drug("DINJAMIO", "AMIODARONE hydrochloride, 50 mg/ ml [fr:AMIODARONE chlorhydrate, 50 mg/ ml]").withFormats(
            new Format("DINJAMIO1A", "3 ml, amp. [fr:3 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJAMOC", "AMOXICILLIN 1g / CLAVULANIC acid 200 mg, powder [fr:AMOXICILLINE 1g / acide CLAVULANIQUE 200 mg, poudre]").withFormats(
            new Format("DINJAMOC1V2", "", Unit.MG)
        ),
        new Drug("DINJAMPI", "AMPICILLIN [fr:AMPICILLINE]").withFormats(
            new Format("DINJAMPI1V", "1 g, powder, vial [fr:1 g, poudre, fl.]", Unit.MG),
            new Format("DINJAMPI5V", "500 mg, powder, vial [fr:500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJAREP", "ARTICAINE 4%/ EPINEPHRINE 1/100 000 [fr:ARTICAINE 4%/ EPINEPHRINE 1/100 000, 1]").withFormats(
            new Format("DINJAREP4C1", "1.7 ml, dent.cartr. [fr:7 ml carp.dent.]", Unit.ML)
        ),
        new Drug("DINJARTS", "ARTESUNATE 60 mg, powder,vial +NaHCO3 5% 1 ml +NaCl 0.9% 5 ml [fr:ARTESUNATE 60 mg, poudre, fl +NaHCO3 5% 1 ml +NaCl 0.9% 5 ml]").withFormats(
            new Format("DINJARTS6V", "", Unit.ML)
        ),
        new Drug("DINJATRB", "ATRACURIUM besilate, 10 mg/ ml [fr:besilate d'ATRACURIUM 10 mg/ ml, 2]").withFormats(
            new Format("DINJATRB2A", "2.5 ml, amp. [fr:5 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJATRO", "ATROPINE sulfate, 1 mg/ ml [fr:ATROPINE sulfate, 1 mg/ ml]").withFormats(
            new Format("DINJATRO1A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJBLEO", "BLEOMYCIN sulfate [fr:BLEOMYCINE sulfate]").withFormats(
            new Format("DINJBLEO1V", "eq 15.000 IU base, powder, vial [fr:éq. 15.000 UI base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJBUPI", "BUPIVACAINE HCl, hyperbaric/spinal, eq.5 mg/ ml base [fr:BUPIVACAINE HCl, hyperbare/rachi, éq.5 mg/ ml base]").withFormats(
            new Format("DINJBUPI2A", "4 ml, amp [fr:4 ml, amp]", Unit.MG)
        ),
        new Drug("DINJCAFC", "CAFFEINE CITRATE [fr:CAFEINE CITRATE]").withFormats(
            new Format("DINJCAFC1A", "10 mg/ ml, eq. 5 mg caffeine base, 1 ml, amp. [fr:10 mg/ ml, éq. 5 mg caféine base, 1 ml, amp.]", Unit.MG),
            new Format("DINJCAFC2A", "20 mg/ ml eq. 10 mg caffeine base, 1 ml, amp. [fr:20 mg/ ml, éq. 10 mg caféine base, 1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJCALG", "CALCIUM GLUCONATE, 100 mg/ ml [fr:CALCIUM GLUCONATE, 100 mg/ ml]").withFormats(
            new Format("DINJCALG1A", "10 ml, amp. [fr:10 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJCAPR", "CAPREOMYCIN sulfate [fr:CAPREOMYCINE sulfate]").withFormats(
            new Format("DINJCAPR1V", "eq. 1 g base, powder, vial [fr:éq. 1 g base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFA", "CEFAZOLIN [fr:CEFAZOLINE]").withFormats(
            new Format("DINJCEFA1V", "1 g, (IV), powder, vial [fr:1 g, (IV), poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFL", "CEFTRIAXONE sodium [fr:CEFTRIAXONE sodique]").withFormats(
            new Format("DINJCEFL1V", "eq.1 g base, powder, vial + lidocaine IM [fr:éq.1 g base, poudre, fl. + lidocaine IM]", Unit.MG),
            new Format("DINJCEFL2V", "eq.250 mg base, powd, vial + lidocaine IM [fr:éq.250 mg base,poudre, fl.+ lidocaine IM]", Unit.MG)
        ),
        new Drug("DINJCEFO", "CEFOTAXIME sodium, eq. [fr:CEFOTAXIME sodique, éq.]").withFormats(
            new Format("DINJCEFO2V", "250 mg base, vial [fr:250 mg base, fl.]", Unit.MG),
            new Format("DINJCEFO5V", "500 mg base, vial [fr:500 mg base, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFT", "CEFTRIAXONE sodium, eq. [fr:CEFTRIAXONE sodique, éq.]").withFormats(
            new Format("DINJCEFT1V", "1 g base, powder, vial [fr:1 g base, poudre, fl.]", Unit.MG),
            new Format("DINJCEFT2V", "250 mg base, powder, vial [fr:250 mg base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFZ", "CEFTAZIDIME [fr:CEFTAZIDIME]").withFormats(
            new Format("DINJCEFZ1V", "1 g, powder, vial [fr:1 g, poudre, fl.]", Unit.MG),
            new Format("DINJCEFZ2V", "2 g, powder, vial [fr:2 g, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCHLO", "CHLORAMPHENICOL [fr:CHLORAMPHENICOL]").withFormats(
            new Format("DINJCHLO1V", "1 g powder, vial [fr:1 g, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCIPR", "CIPROFLOXACIN salt, eq. 2 mg/ ml base,100 [fr:sel de CIPROFLOXACINE, éq.2 mg/ ml base,100]").withFormats(
            new Format("DINJCIPR2FBF", "ml,flex.bag PVC free [fr:ml,poche sple ssPVC]", Unit.MG),
            new Format("DINJCIPR2SRF", "ml,semi-r.bot PVCfree [fr:ml,fl.semi-r. ssPVC]", Unit.MG)
        ),
        new Drug("DINJCLIN", "CLINDAMYCIN phosphate, eq. 150 mg base/ ml [fr:CLINDAMYCINE phosphate, éq. 150 mg base/ ml]").withFormats(
            new Format("DINJCLIN3A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJCLON", "CLONIDINE, 0.15 mg/ ml [fr:CLONIDINE , 0.15 mg/ ml]").withFormats(
            new Format("DINJCLON1A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJCLOX", "CLOXACILLIN sodium [fr:CLOXACILLINE sodique]").withFormats(
            new Format("DINJCLOX5VV", "eq. 500 mg base, powder, vial IV [fr:éq. 500 mg base, poudre, fl. IV]", Unit.MG)
        ),
        new Drug("DINJCOLI", "COLISTIMETHATE sodium [fr:COLISTIMETHATE sodique]").withFormats(
            new Format("DINJCOLI1V", "1 MIU, powder, vial [fr:1 MUI, poudre, fl.]", Unit.MG),
            new Format("DINJCOLI2V", "2 M IU, powder, vial, for infusion [fr:2 M UI, poudre, flacon, pour perf.]", Unit.MG)
        ),
        new Drug("DINJCOTR", "COTRIMOXAZOLE, 80 mg/16 mg/ ml [fr:COTRIMOXAZOLE, 80 mg/16 mg/ ml]").withFormats(
            new Format("DINJCOTR4A", "5 ml for infusion, amp. [fr:5 ml pour perfusion, amp.]", Unit.MG)
        ),
        new Drug("DINJDEFE", "DEFEROXAMINE (desferrioxamine) mesilate [fr:DEFEROXAMINE (desferrioxamine) mesilate]").withFormats(
            new Format("DINJDEFE5V", "500 mg, powder, vial [fr:500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJDEXA", "DEXAMETHASONE phosphate, 4 mg/ ml [fr:DEXAMETHASONE phosphate, 4 mg/ ml]").withFormats(
            new Format("DINJDEXA4A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJDIAZ", "DIAZEPAM, 5 mg/ ml, 2 ml [fr:DIAZEPAM, 5 mg/ ml, 2 ml]").withFormats(
            new Format("DINJDIAZ1A", "amp. [fr:amp.]", Unit.MG),
            new Format("DINJDIAZ1AE", "emulsion, amp. [fr:émulsion, amp.]", Unit.MG)
        ),
        new Drug("DINJDICL", "DICLOFENAC sodium, 25 mg/ ml [fr:DICLOFENAC sodique, 25 mg/ ml]").withFormats(
            new Format("DINJDICL7A", "3 ml, amp. [fr:3 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJDIGO", "DIGOXIN, 0.25 mg/ ml [fr:DIGOXINE, 0,25 mg/ ml]").withFormats(
            new Format("DINJDIGO5A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJDILT", "DILTIAZEM hydrochloride [fr:DILTIAZEM chlorhydrate]").withFormats(
            new Format("DINJDILT2V", "25 mg, powder, vial [fr:25 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJDOBU", "DOBUTAMINE HCl., eq.12,5 mg/ ml base [fr:DOBUTAMINE HCl.,éq. 12,5 mg/ ml base]").withFormats(
            new Format("DINJDOBU2A", "20 ml, sol for infusion [fr:20 ml,sol pour perfusion]", Unit.MG)
        ),
        new Drug("DINJDOPA", "DOPAMINE hydrochloride, 40 mg/ ml [fr:DOPAMINE chlorhydrate, 40 mg/ ml]").withFormats(
            new Format("DINJDOPA2A", "5 ml, amp. [fr:5 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJDOPL", "DOXORUBICIN HCl, pegylated liposomal, 2 mg/ ml [fr:DOXORUBICINE HCl, pégylée liposomale, 2 mg/ ml]").withFormats(
            new Format("DINJDOPL2V", "10 ml vial [fr:10 ml fl.]", Unit.MG),
            new Format("DINJDOPL5V", "25 ml vial [fr:25 ml fl.]", Unit.MG)
        ),
        new Drug("DINJDOXO", " [fr:DOXORUBICINE chlorhydrate]").withFormats(
            new Format("DINJDOXO1V", "DOXORUBICIN hydrochloride, 10 mg, powder, vial [fr:10 mg, poudre, fl.]", Unit.MG),
            new Format("DINJDOXO1V5", "DOXORUBICINE hydrochloride, 2 mg/ ml, 5 ml, vial [fr:2 mg/ ml, 5 ml, fl.]", Unit.MG)
        ),
        new Drug("DINJEFLO", "EFLORNITHINE hydrochloride, eq. 200 mg/ ml base [fr:EFLORNITHINE chlorhydrate, éq. 200 mg/ ml base]").withFormats(
            new Format("DINJEFLO2V", "100 ml, vial [fr:100 ml, fl.]", Unit.MG)
        ),
        new Drug("DINJENOX", "ENOXAPARIN sodium [fr:ENOXAPARINE sodique]").withFormats(
            new Format("DINJENOX10S", "10,000IU/1 ml, syringe [fr:10 000UI/1 ml, seringue]", Unit.ML),
            new Format("DINJENOX20S", "2,000IU/0.2 ml, syringe [fr:2 000UI/0,2 ml, seringue]", Unit.ML),
            new Format("DINJENOX40S", "4,000 IU/0.4 ml, syringe [fr:4 000UI/0,4 ml, seringue]", Unit.ML),
            new Format("DINJENOX60S", "6,000IU/0.6 ml, syringe [fr:6 000UI/0,6 ml, seringue]", Unit.ML)
        ),
        new Drug("DINJEPHE", "EPHEDRINE hydrochloride, 30 mg/ ml [fr:EPHEDRINE chlorhydrate, 30 mg/ ml]").withFormats(
            new Format("DINJEPHE3A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJEPIN", "EPINEPHRINE (adrenaline) [fr:EPINEPHRINE (adrenaline) tartrate,éq.1 mg/ ml base, 1 ml amp]").withFormats(
            new Format("DINJEPIN1AM", "tartrate, eq.1 mg/ ml base,1 ml amp IM [fr:IM]", Unit.MG),
            new Format("DINJEPIN1AV", "tartrate,eq.1 mg/ ml base,1 ml amp IV [fr:IV]", Unit.MG)
        ),
        new Drug("DINJEPOA", "EPOETIN ALFA, 10 000 IU/ ml [fr:EPOETINE ALFA, 10 000 UI/ ml]").withFormats(
            new Format("DINJEPOA1S", "1 ml, graduated syringe [fr:1 ml, seringue graduée]", Unit.MG)
        ),
        new Drug("DINJERYT", "ERYTHROMYCIN lactobionate, eq. to 1 g base, pdr, vial [fr:ERYTHROMYCINE lactobionate, éq. à 1 g base, pdr, fl.]").withFormats(
            new Format("DINJERYT1V", "", Unit.MG)
        ),
        new Drug("DINJETAM", "ETAMSYLATE, 125 mg/ ml [fr:ETAMSYLATE, 125 mg/ ml]").withFormats(
            new Format("DINJETAM2A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJETON", "ETONOGESTREL implant 1 x 68 mg, with applicator s.u. [fr:ETONOGESTREL implant 1 x 68 mg, avec applicateur u.u.]").withFormats(
            new Format("DINJETON6I", "", Unit.MG)
        ),
        new Drug("DINJFENT", "FENTANYL citrate, eq. 0.05 mg/ ml base [fr:FENTANYL citrate, éq. 0,05 mg/ ml base]").withFormats(
            new Format("DINJFENT1A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJFERC", "FERRIC carboxymaltose, eq. 50 mg/ ml iron [fr:carboxymaltose FERRIQUE, eq. 50 mg/ ml fer]").withFormats(
            new Format("DINJFERC1A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJFLUC", "FLUCONAZOLE, 2 mg/ ml, 100 ml [fr:FLUCONAZOLE, 2 mg/ ml, 100 ml]").withFormats(
            new Format("DINJFLUC2FBF", "flexible bag PVC free [fr:poche souple sans PVC]", Unit.MG),
            new Format("DINJFLUC2SRF", "semi-rigid bot. PVC free [fr:fl. semi-rigide sans PVC]", Unit.MG)
        ),
        new Drug("DINJFLUM", "FLUMAZENIL, 0.1 mg/ ml [fr:FLUMAZENIL]").withFormats(
            new Format("DINJFLUM1A", "10 ml, amp. [fr:0,1 mg/1 ml, 10 ml, amp.]", Unit.MG),
            new Format("DINJFLUM5A", "5 ml, amp. [fr:0.1 mg/ ml, 5 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJFURO", "FUROSEMIDE, 10 mg/ ml [fr:FUROSEMIDE, 10 mg/ ml]").withFormats(
            new Format("DINJFURO2A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJGANC", "GANCICLOVIR sodium [fr:GANCICLOVIR sodique]").withFormats(
            new Format("DINJGANC5V", "eq.500 mg base, powder, vial fr infusion [fr:éq.500 mg base, poudre, flacon perfusion]", Unit.MG)
        ),
        new Drug("DINJGENT", "GENTAMICIN sulfate, eq. [fr:GENTAMICINE sulfate]").withFormats(
            new Format("DINJGENT2A", "10 mg/ ml base, 2 ml, amp. [fr:éq. 10 mg/ ml base, 2 ml, amp.]", Unit.MG),
            new Format("DINJGENT2V", "10 mg/ ml base, 2 ml, vial [fr:éq. 10 mg/ ml base, 2 ml, fl.]", Unit.MG),
            new Format("DINJGENT8A", "40 mg/ ml base, 2 ml, amp. [fr:eq. 40 mg/ ml base, 2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJGLUC", "GLUCOSE [fr:GLUCOSE]").withFormats(
            new Format("DINJGLUC1A1", "hypertonic, 10%, 10 ml, amp [fr:hypertonique, 10%, 10 ml, amp]", Unit.ML),
            new Format("DINJGLUC3A2", "HYPER, 30%, 20 ml, amp. [fr:HYPERTONIQUE, 30%, 20 ml, amp.]", Unit.ML),
            new Format("DINJGLUC5V5", "hypertonic, 50%, 50 ml, vial [fr:hypertonique, 50%, 50 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJGLYC", "GLYCOPYRRONIUM bromide, 0.2 mg/ ml [fr:GLYCOPYRRONIUM bromure, 0.2 mg/ ml]").withFormats(
            new Format("DINJGLYC2A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJHALP", "").withFormats(
            new Format("DINJHALP5A", "HALOPERIDOL, 5 mg/ ml, 1 ml, amp. [fr:HALOPERIDOL, 5 mg/ ml, 1 ml, amp.]", Unit.MG),
            new Format("DINJHALP5AD", "HALOPERIDOL decanoate, 50 mg/ ml, 1 ml, amp. [fr:HALOPERIDOL decanoate, 50 mg/ ml, 1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJHEPA", "HEPARIN SODIUM, 5 000 IU/ ml [fr:HEPARINE SODIQUE, 5 000 UI/ ml]").withFormats(
            new Format("DINJHEPA2A", "5 ml, amp. [fr:5 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJHYDA", "HYDRALAZINE hydrochloride [fr:HYDRALAZINE chlorhydrate]").withFormats(
            new Format("DINJHYDA2A", "20 mg, powder, amp. [fr:20 mg, poudre, amp.]", Unit.MG)
        ),
        new Drug("DINJHYDR", "HYDROCORTISONE [fr:HYDROCORTISONE]").withFormats(
            new Format("DINJHYDR1V", "sodium succinate, eq.100 mg base, powder, vial [fr:succinate sodique, eq.100 mg base, poudre,fl]", Unit.MG),
            new Format("DINJHYDR1VS", "sod succ, eq.100 mg base, powder,vial +solvent [fr:succ sod, éq. 100 mg base,fl. pdre + solvant]", Unit.MG)
        ),
        new Drug("DINJHYOS", "HYOSCINE BUTYLBROMIDE (scopolamine butylbrom) [fr:BUTYLBROMURE HYOSCINE (butylbrom.scopolamine)]").withFormats(
            new Format("DINJHYOS2A", "20 mg/1 ml,amp [fr:20 mg/1 ml,amp]", Unit.ML)
        ),
        new Drug("DINJIMCI", "IMIPENEM 500 mg / CILASTATIN sodium 500 mg, powder, vial [fr:IMIPENEME 500 mg / CILASTATIN sodium 500 mg, poudre, fl.]").withFormats(
            new Format("DINJIMCI55V", "", Unit.MG)
        ),
        new Drug("DINJINSA", "INSULIN [fr:INSULINE]").withFormats(
            new Format("DINJINSAB3APL", "LISPRO, BIPHASIC 25-75 IU/ ml, 3 ml, autoinj.pref. L [fr:LISPRO, BIPHASIQUE 25-75 UI/ ml, 3 ml,stylo prér. L]", Unit.MG),
            new Format("DINJINSAB3APN", "ASPART, BIPHASIC 30-70 IU/ ml, 3 ml, autoinj.pref. N [fr:ASPART, BIPHASIQUE 30-70 UI/ ml, 3 ml,stylo prér. N]", Unit.MG),
            new Format("DINJINSAL1VS", "GLARGINE, LONG 100 IU/ ml, 10 ml, vial S [fr:GLARGINE, LENTE 100 UI/ ml, 10 ml, fl. S]", Unit.MG),
            new Format("DINJINSAL3APS", "GLARGINE, LONG, 100 IU/ ml, 3 ml, autoinjector pref. S [fr:GLARGINE, LENTE, 100 UI/ ml, 3 ml, stylo prérempli S]", Unit.MG),
            new Format("DINJINSAU3APL", "LISPRO, ULTRARAPID 100 UI/ ml, 3 ml, autoinject.pref.L [fr:LISPRO, ULTRARAPIDE 100 UI/ ml, 3 ml, stylo prér. L]", Unit.MG),
            new Format("DINJINSAU3APN", "ASPART, ULTRARAPID 100 UI/ ml, 3 ml, autoinject.pref.N [fr:ASPART, ULTRARAPIDE 100 UI/ ml, 3 ml, stylo prér. N]", Unit.MG)
        ),
        new Drug("DINJINSH", "INSULIN HUMAN [fr:INSULINE HUMAINE]").withFormats(
            new Format("DINJINSHB1VL", "BIPHASIC 30-70 IU/ ml, 10 ml, vial L [fr:BIPHASIQUE 30-70 UI/ ml, 10 ml, fl. L]", Unit.MG),
            new Format("DINJINSHB1VN", "BIPHASIC 30-70 IU/ ml, 10 ml, vial N [fr:BIPHASIQUE 30-70 UI/ ml, 10 ml, fl. N]", Unit.MG),
            new Format("DINJINSHB1VS", "BIPHASIC 30-70 IU/ ml, 10 ml, vial S [fr:BIPHASIQUE 30-70 UI/ ml, 10 ml, fl. S]", Unit.MG),
            new Format("DINJINSHI1VN", "ISOPHANE (NPH) 100 UI/ ml, 10 ml, vial N [fr:ISOPHANE (NPH) 100 UI/ ml, 10 ml, fl. N]", Unit.MG),
            new Format("DINJINSHI1VS", "ISOPHANE (NPH) 100 UI/ ml, 10 ml, vial S [fr:ISOPHANE (NPH) 100 UI/ ml, 10 ml, fl. S]", Unit.MG),
            new Format("DINJINSHI3APN", "ISOPHANE (NPH) 100 UI/ ml, 3 ml, autoinj.pref.N [fr:ISOPHANE (NPH) 100 UI/ ml, 3 ml,stylo prér.N]", Unit.MG),
            new Format("DINJINSHR1VN", "RAPID 100 IU/ ml, 10 ml, vial N [fr:RAPIDE 100 UI/ ml, 10 ml, fl. N]", Unit.MG),
            new Format("DINJINSHR1VS", "RAPID 100 IU/ ml, 10 ml, vial S [fr:RAPIDE 100 UI/ ml, 10 ml, fl. S]", Unit.MG)
        ),
        new Drug("DINJISOB", "ISOSORBIDE DINITRATE, 1 mg/ ml [fr:ISOSORBIDE DINITRATE, 1 mg/ ml]").withFormats(
            new Format("DINJISOB1A", "10 ml, amp. [fr:10 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJKANA", "KANAMYCIN sulfate, eq. [fr:KANAMYCINE sulfate, éq.]").withFormats(
            new Format("DINJKANA1A4", "0.250g/ ml base, 4 ml, amp. [fr:0.250g/ ml base, 4 ml, amp.]", Unit.MG),
            new Format("DINJKANA1V", "1 g base, powder, vial [fr:1 g base, poudre, fl.]", Unit.MG),
            new Format("DINJKANA5A2", "0.250g/ ml base, 2 ml, amp. [fr:0.250g/ ml base, 2 ml, amp.]", Unit.MG),
            new Format("DINJKANA5V", "0.5 g base, powder, vial [fr:0.5 g base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJKETA", "KETAMINE hydrochloride, eq. 50 mg/ ml base [fr:KETAMINE chlorhydrate, éq. 50 mg/ ml base]").withFormats(
            new Format("DINJKETA2A", "5 ml, amp. [fr:5 ml, amp.]", Unit.MG),
            new Format("DINJKETA5V", "10 ml, vial [fr:10 ml, fl.]", Unit.MG)
        ),
        new Drug("DINJLABE", "LABETALOL hydrochloride, 5 mg/ ml [fr:LABETALOL chlorhydrate, 5 mg/ ml]").withFormats(
            new Format("DINJLABE1A", "20 ml amp. [fr:20 ml amp.]", Unit.MG)
        ),
        new Drug("DINJLEVB", "LEVOBUPIVACAINE hydrochloride [fr:LEVOBUPIVACAINE chlorhydrate éq.]").withFormats(
            new Format("DINJLEVB2A", "eq.2.5 mg/ ml base, 10 ml, amp. [fr:2,5 mg/ ml base, 10 ml, amp.]", Unit.MG),
            new Format("DINJLEVB5A", "eq. 5 mg/ ml base,10 ml, amp [fr:5 mg/ ml base, 10 ml, amp]", Unit.MG)
        ),
        new Drug("DINJLEVE", "LEVETIRACETAM, 100 mg/ ml [fr:LEVETIRACETAM, 100 mg/ ml]").withFormats(
            new Format("DINJLEVE5V", "5 ml, vial [fr:5 ml, fl.]", Unit.MG)
        ),
        new Drug("DINJLEVN", "LEVONORGESTREL implant 2 x 75 mg (Jadelle) + trocar [fr:LEVONORGESTREL implant 2 x 75 mg (Jadelle) + trocart]").withFormats(
            new Format("DINJLEVN15I", "", Unit.MG)
        ),
        new Drug("DINJLIDE", "LIDOCAINE [fr:LIDOCAINE]").withFormats(
            new Format("DINJLIDE1C2", "1% / EPINEPHRINE 1/200,000, 20 ml, vial [fr:1% / EPINEPHRINE 1/200 000, 20 ml, fl.]", Unit.ML),
            new Format("DINJLIDE2C1", "2% / EPINEPHRINE 1/80 000, 1.8 ml, cart. [fr:2% / EPINEPHRINE 1/80 000, 1,8 ml, cart.]", Unit.ML)
        ),
        new Drug("DINJLIDO", "LIDOCAINE hydrochloride [fr:LIDOCAINE chlorhydrate]").withFormats(
            new Format("DINJLIDO1A1", "1%, preservative-free, 10 ml, amp [fr:1%, sans conservateur, 10 ml, amp]", Unit.ML),
            new Format("DINJLIDO1A5", "1%, preservative-free,5 ml,plast.amp [fr:1% ,sans conservateur, 5 ml,amp.plast]", Unit.ML),
            new Format("DINJLIDO1V2", "1%, preservative-free, 20 ml, vial [fr:1%, sans conservateur, 20 ml, fl.]", Unit.ML),
            new Format("DINJLIDO2V2", "2%, preservative-free, 20 ml, vial [fr:2%, sans conservateur, 20 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJLIPE", "LIPID emulsion, 20% [fr:émulsion LIPIDIQUE, 20%]").withFormats(
            new Format("DINJLIPE2FBF2", "250 ml, flex. bag, PVC free [fr:250 ml bot., poche souple, sans PVC]", Unit.ML)
        ),
        new Drug("DINJMAGS", "MAGNESIUM sulfate, 0.5 g/ ml [fr:MAGNESIUM sulfate, 0,5 g/ ml]").withFormats(
            new Format("DINJMAGS5A", "10 ml, amp. [fr:10 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJMEDR", "MEDROXYPROGESTERONE acetate [fr:MEDROXYPROGESTERONE acétate]").withFormats(
            new Format("DINJMEDR1S", "150 mg, 1 ml, syringe [fr:150 mg, 1 ml, seringue]", Unit.MG),
            new Format("DINJMEDR1V", "150 mg, 1 ml, vial [fr:150 mg, 1 ml, fl.]", Unit.MG),
            new Format("DINJMEDR6IP", "104 mg/0,65 ml,injector prefilled [fr:104 mg/0,65 ml,injecteur prérempl]", Unit.MG)
        ),
        new Drug("DINJMEGA", "MEGLUMINE ANTIMONIATE, pentaval. antimony 81 mg/ ml [fr:MEGLUMINE ANTIMONIATE, antimoine pentaval. 81 mg/ ml]").withFormats(
            new Format("DINJMEGA4A", "5 ml, amp [fr:5 ml, amp]", Unit.MG)
        ),
        new Drug("DINJMELA", "MELARSOPROL, 36 mg/ ml [fr:MELARSOPROL, 36 mg/ ml]").withFormats(
            new Format("DINJMELA3A5", "5 ml, amp. [fr:5 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJMERG", "METHYLERGOMETRINE maleate, 0.2 mg/ ml [fr:METHYLERGOMETRINE maleate, 0,2 mg/ ml]").withFormats(
            new Format("DINJMERG2A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJMERO", "MEROPENEM [fr:MEROPENEME]").withFormats(
            new Format("DINJMERO1V", "1 g, powder, vial [fr:1 g, poudre, fl]", Unit.MG),
            new Format("DINJMERO5V", "500 mg, powder, vial [fr:500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJMETN", "METRONIDAZOLE, 5 mg/ ml, 100 ml [fr:METRONIDAZOLE, 5 mg/ ml, 100 ml]").withFormats(
            new Format("DINJMETN5FBF", "flex. bag PVC free [fr:poche souple sans PVC]", Unit.MG),
            new Format("DINJMETN5SRF", "semi-rigid bot. PVC free [fr:fl. semi-rigide sans PVC]", Unit.MG)
        ),
        new Drug("DINJMETO", "METOCLOPRAMIDE hydrochloride, 5 mg/ ml [fr:METOCLOPRAMIDE chlorhydrate, 5 mg/ ml]").withFormats(
            new Format("DINJMETO1A", "2 ml, amp. [fr:2 ml, amp]", Unit.MG)
        ),
        new Drug("DINJMIDA", "MIDAZOLAM, 1 mg / ml [fr:MIDAZOLAM, 1 mg / ml]").withFormats(
            new Format("DINJMIDA5A", "5 ml, amp [fr:5 ml, amp]", Unit.MG)
        ),
        new Drug("DINJMORP", "MORPHINE hydrochloride, 10 mg/ ml [fr:MORPHINE chlorhydrate, 10 mg/ ml]").withFormats(
            new Format("DINJMORP1A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJNADR", " [fr:NADROPARINE calcique]").withFormats(
            new Format("DINJNADR2S", "NADROPARIN calcium, 1900 IU / 0.2 ml, syringe [fr:1900 UI / 0,2 ml, seringue]", Unit.ML),
            new Format("DINJNADR3S", "NADROPARIN calcium, 2850 IU / 0.3 ml, syringe [fr:2850 UI / 0,3 ml, seringue]", Unit.ML),
            new Format("DINJNADR4S", "NADROPARIN calcium, 3800 IU / 0.4 ml, syringe [fr:3800 UI / 0,4 ml, seringue]", Unit.ML),
            new Format("DINJNADR5S", "NADROPARINE calcium, 5700 UI / 0.6 ml, syringe [fr:5700 UI / 0,6 ml, seringue]", Unit.ML)
        ),
        new Drug("DINJNALO", "NALOXONE hydrochloride, 0.4 mg/ ml [fr:NALOXONE chlorhydrate, 0,4 mg/ ml]").withFormats(
            new Format("DINJNALO4A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJNEOS", "NEOSTIGMINE methylsulfate, 2.5 mg/ ml [fr:NEOSTIGMINE méthylsulfate, 2,5 mg/ ml]").withFormats(
            new Format("DINJNEOS2A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJNEPI", "NOREPINEPHRINE (noradrenaline) tartrate,eq.1 mg/ ml base [fr:NOREPINEPHRINE (noradrénaline) tartrate,éq.1 mg/ ml base]").withFormats(
            new Format("DINJNEPI4AV", "4 ml [fr:4 ml]", Unit.MG)
        ),
        new Drug("DINJNICA", "NICARDIPINE hydrochloride, 1 [fr:NICARDIPINE chlorhydrate, 1]").withFormats(
            new Format("DINJNICA1A", "mg / ml, 10 ml, amp. [fr:mg / ml, 10 ml, amp.]", Unit.MG),
            new Format("DINJNICA5A", "mg/1 ml, 5 ml, amp. [fr:mg/1 ml, 5 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJOMEP", "OMEPRAZOLE sodium [fr:OMEPRAZOLE sodique]").withFormats(
            new Format("DINJOMEP4V", "eq. 40 mg base, powder, vial, fr infusion [fr:éq.40 mg base, poudre, fl. pr perfusion]", Unit.MG)
        ),
        new Drug("DINJONDA", "ONDANSETRON hydrochloride, eq. 2 mg/ ml base [fr:ONDANSETRON chlorhydrate, éq. 2 mg/ ml base]").withFormats(
            new Format("DINJONDA4A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJOXYT", "OXYTOCIN, 10 IU/ ml [fr:OXYTOCINE, 10 UI/ ml]").withFormats(
            new Format("DINJOXYT1A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJPACL", "PACLITAXEL 6 mg/ ml sol. to be diluted [fr:PACLITAXEL 6 mg/ ml sol. a diluer]").withFormats(
            new Format("DINJPACL1V", "16.7 ml, bot. [fr:16.7 ml, fl.]", Unit.MG)
        ),
        new Drug("DINJPARA", "PARACETAMOL [fr:PARACETAMOL]").withFormats(
            new Format("DINJPARA1B", "(acetaminophen), 10 mg/ ml, 100 ml, bot. [fr:(acétaminophène), 10 mg/ ml, 100 ml, fl.]", Unit.MG),
            new Format("DINJPARA1FBF", "(acetaminophen),10 mg/ ml,100 ml, flex.bag PVC free [fr:(acétaminophène),10 mg/ ml,100 ml, poche s. ss PVC]", Unit.MG),
            new Format("DINJPARA5B", "(acetaminophen),10 mg/ ml, 50 ml, bot. [fr:(acétaminophène),10 mg/ ml, 50 ml, fl.]", Unit.MG),
            new Format("DINJPARA5FBF", "(acetaminophen),10 mg/ ml, 50 ml, flex.bag PVC free [fr:(acétaminophène),10 mg/ ml, 50 ml, poche s. ss PVC]", Unit.MG)
        ),
        new Drug("DINJPARO", "PAROMOMYCIN sulfate, eq. 375 mg/ ml base [fr:PAROMOMYCINE sulfate, éq. 375 mg/ ml base]").withFormats(
            new Format("DINJPARO1A", "2 ml, amp [fr:2 ml, amp]", Unit.MG)
        ),
        new Drug("DINJPENB", "BENZATHINE [fr:BENZATHINE BENZYLPENICILLINE]").withFormats(
            new Format("DINJPENB1V", "BENZYLPENICILLIN, 1.2 M IU, powder, vial [fr:1,2 M UI, poudre, fl.]", Unit.MG),
            new Format("DINJPENB1VS", "BENZYLPENICILLIN, 1.2 M IU, powder, vial+ solvent [fr:1,2 M UI, poudre, fl.+solvant]", Unit.MG),
            new Format("DINJPENB2V", "BENZYLPENICILLIN , 2.4 M IU, powder, vial [fr:2,4 M UI, poudre, fl.]", Unit.MG),
            new Format("DINJPENB2VS", "BENZYLPENICILLIN, 2.4 M IU, powder, vial+ solvent [fr:2,4 M UI, poudre, fl.+solvant]", Unit.MG)
        ),
        new Drug("DINJPENG", "BENZYLPENICILLIN (peni G, crystal peni) [fr:BENZYLPENICILLINE (peni G, cristal peni)]").withFormats(
            new Format("DINJPENG1V", "1 MIU, powder,vial [fr:1 MUI, poudre, fl]", Unit.MG),
            new Format("DINJPENG5V", "5 MIU, powder,vial [fr:5 MUI, poudre, fl]", Unit.MG)
        ),
        new Drug("DINJPENT", "PENTAMIDINE isetionate [fr:PENTAMIDINE isetionate]").withFormats(
            new Format("DINJPENT3V", "300 mg, powder, vial [fr:300 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJPHEE", "PHENYLEPHRINE hydrochloride, eq. 50 µg base/ ml [fr:PHENYLEPHRINE chlorhydrate, éq. 50 µg base/ ml]").withFormats(
            new Format("DINJPHEE5A", "10 ml amp. [fr:10 ml amp.]", Unit.MG)
        ),
        new Drug("DINJPHEN", "PHENOBARBITAL sodium, 200 mg/ ml [fr:PHENOBARBITAL sodique, 200 mg/ ml]").withFormats(
            new Format("DINJPHEN2A1", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJPHEY", "PHENYTOIN sodium, 50 mg/ ml, 5 ml [fr:PHENYTOINE sodique, 50 mg/ ml, 5 ml]").withFormats(
            new Format("DINJPHEY2A", "amp. [fr:amp.]", Unit.MG),
            new Format("DINJPHEY2V", "vial [fr:fl.]", Unit.MG)
        ),
        new Drug("DINJPHLT", "PHLOROGLUCINOL10 mg/ ml/TRIMETHYLPHLOROGLUCINOL10µg/ ml [fr:PHLOROGLUCINOL10 mg/ ml/TRIMETHYLPHLOROGLUCINOL10µg/ ml]").withFormats(
            new Format("DINJPHLT44A", "4 ml,amp [fr:4 ml,amp]", Unit.MG)
        ),
        new Drug("DINJPHYT", "PHYTOMENADIONE (vitamin K1), 10 mg/ ml (2 mg/0.2 ml) [fr:PHYTOMENADIONE (vitamine K1), 10 mg/ ml (2 mg/0,2 ml)]").withFormats(
            new Format("DINJPHYT2AN", "0.2 ml amp. [fr:0.2 ml amp.]", Unit.ML)
        ),
        new Drug("DINJPITA", "PIPERACILLIN 4g / TAZOBACTAM 500 mg, powder, vial for inf. [fr:PIPERACILLINE 4g / TAZOBACTAM 500 mg, poudre, fl. pour perf.]").withFormats(
            new Format("DINJPITA45V", "", Unit.MG)
        ),
        new Drug("DINJPOTC", "POTASSIUM chloride, 100 mg/ ml [fr:POTASSIUM chlorure, 100 mg/ ml]").withFormats(
            new Format("DINJPOTC1A", "10 ml, amp. [fr:10 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJPRAL", "PRALIDOXIME, 2 %, vial powder + amp. solvant [fr:PRALIDOXIME, 2 % flacons poudre+ ampoules solvant]").withFormats(
            new Format("DINJPRAL2A1S", "10 ml. [fr:10 ml.]", Unit.MG)
        ),
        new Drug("DINJPROM", "PROMETHAZINE hydrochloride, eq. 25 mg/ ml base [fr:PROMETHAZINE chlorhydrate, éq. 25 mg/ ml base]").withFormats(
            new Format("DINJPROM2A", "1 ml, amp. [fr:1 ml, amp.]", Unit.MG),
            new Format("DINJPROM5A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJPROP", "PROPOFOL, 10 mg/ ml [fr:PROPOFOL, 10 mg/ ml]").withFormats(
            new Format("DINJPROP2AE", "20 ml, emulsion, amp. [fr:20 ml, émulsion, amp.]", Unit.MG)
        ),
        new Drug("DINJPROT", "PROTAMINE sulfate, 10 mg/ ml [fr:PROTAMINE sulfate, 10 mg/ ml]").withFormats(
            new Format("DINJPROT5A", "5 ml, amp. [fr:5 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJRANI", "RANITIDINE, 25 mg/ ml [fr:RANITIDINE, 25 mg/ ml]").withFormats(
            new Format("DINJRANI5A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJRIBA", "RIBAVIRIN, 100 mg/ ml [fr:RIBAVIRINE, 100 mg/ ml]").withFormats(
            new Format("DINJRIBA1A", "12 ml, amp. [fr:12 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJRIFA", "RIFAMPICIN (R) [fr:RIFAMPICINE (R)]").withFormats(
            new Format("DINJRIFA6VS", "600 mg, powder, vial + solvent [fr:600 mg, poudre, fl. + solvant]", Unit.MG)
        ),
        new Drug("DINJSODB", "SODIUM BICARBONATE, 8.4%, 1 mEq/ ml [fr:SODIUM BICARBONATE, 8,4%, 1 mEq/ ml]").withFormats(
            new Format("DINJSODB8A1", "10 ml, amp. [fr:10 ml, amp.]", Unit.ML),
            new Format("DINJSODB8A2", "20 ml, amp. [fr:20 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJSODC", "SODIUM").withFormats(
            new Format("DINJSODC1A1", "chloride 10 %, 10 ml, amp. [fr:Chlorure de SODIUM, 10 %, 10 ml, amp.]", Unit.MG),
            new Format("DINJSODC2A1", "chloride, hypertonic, 20%, 10 ml, amp. [fr:SODIUM chlorure, hypertonique, 20%, 10 ml, amp.]", Unit.ML),
            new Format("DINJSODC9A1", "chloride, 0.9%, 10 ml, amp. [fr:SODIUM chlorure 0.9%, 10 ml, amp.]", Unit.ML),
            new Format("DINJSODC9A5", "chloride, 0.9%, 5 ml, plastic amp. [fr:SODIUM chlorure, 0,9%, 5 ml, amp.]", Unit.ML),
            new Format("DINJSODC9AP1", "chloride, 0.9%, 10 ml, plastic amp. [fr:SODIUM chlorure 0.9%, 10 ml, amp. plastique]", Unit.ML)
        ),
        new Drug("DINJSPEC", "SPECTINOMYCIN hydrochloride [fr:SPECTINOMYCINE chlorhydrate]").withFormats(
            new Format("DINJSPEC2V", "eq. 2 g base, powder, vial [fr:éq. 2 g base, poudre, fl.]", Unit.MG),
            new Format("DINJSPEC2VS", "eq.2g base, powder,vial+SOLVENT [fr:éq.2g base, poudre,fl.+ SOLVANT]", Unit.MG)
        ),
        new Drug("DINJSSGL", "SODIUM STIBOGLUCONATE, pentaval.antimony 100 mg/ [fr:SODIUM STIBOGLUCONATE, antimoine pentaval. 100 mg/]").withFormats(
            new Format("DINJSSGL1V1", "ml 100 ml vial [fr:ml 100 ml fl]", Unit.MG),
            new Format("DINJSSGL1V3", "ml, 30 ml vial [fr:ml, 30 ml fl]", Unit.MG)
        ),
        new Drug("DINJSTRE", "STREPTOMYCIN sulfate [fr:STREPTOMYCINE sulfate]").withFormats(
            new Format("DINJSTRE1V", "eq. 1 g base, powder, vial [fr:éq. 1 g base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJSTRK", "STREPTOKINASE [fr:STREPTOKINASE]").withFormats(
            new Format("DINJSTRK1V", "1.500.000 IU, powder, vial [fr:1.500.000 IU, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJSUXC", "SUXAMETHONIUM chloride, 50 mg/ ml [fr:SUXAMETHONIUM chlorure, 50 mg/ ml]").withFormats(
            new Format("DINJSUXC1A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJTHIA", "THIAMINE (vitamin B1), 50 mg/ ml [fr:THIAMINE (vitamine B1), 50 mg/ ml]").withFormats(
            new Format("DINJTHIA1A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJTHIO", "THIOPENTAL sodium [fr:THIOPENTAL sodique]").withFormats(
            new Format("DINJTHIO5V", "500 mg, powder, vial [fr:500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJTRAM", "TRAMADOL hydrochloride, 50 mg/ ml [fr:TRAMADOL chlorhydrate, 50 mg/ ml]").withFormats(
            new Format("DINJTRAM1A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJTRAN", "TRANEXAMIC ACID, 100 mg/ ml [fr:ACIDE TRANEXAMIQUE, 100 mg / ml]").withFormats(
            new Format("DINJTRAN5A", "5 ml amp. [fr:5 ml amp.]", Unit.MG)
        ),
        new Drug("DINJUROK", "UROKINASE [fr:UROKINASE]").withFormats(
            new Format("DINJUROK1V", "100000 IU, powder, vial [fr:100000 UI, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJVALP", "VALPROATE SODIUM , 100 mg/ ml [fr:VALPROATE DE SODIUM, 100 mg/ ml]").withFormats(
            new Format("DINJVALP4A", "4 ml amp. [fr:4 ml amp.]", Unit.MG)
        ),
        new Drug("DINJVANC", "VANCOMYCIN hydrocloride, eq. [fr:VANCOMYCINE]").withFormats(
            new Format("DINJVANC1V", "1g base, powder, vial [fr:chlorydrate, éq. 1g base, poudre, fl.]", Unit.MG),
            new Format("DINJVANC5V", "500 mg base, powder, vial [fr:chlorhydrate, éq. 500 mg base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJVECB", "VECURONIUM bromide [fr:VECURONIUM bromure]").withFormats(
            new Format("DINJVECB1V", "10 mg, powder, vial [fr:10 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJVERA", "VERAPAMIL hydrochloride, 2.5 mg/ ml [fr:VERAPAMIL chlorhydrate, 2.5 mg/ ml]").withFormats(
            new Format("DINJVERA2A", "2 ml, amp. [fr:2 ml, amp.]", Unit.MG)
        ),
        new Drug("DINJVINC", "VINCRISTINE sulfate, 1 mg/ ml [fr:VINCRISTINE sulfate, 1 mg/ ml]").withFormats(
            new Format("DINJVINC1V", "1 ml, vial [fr:1 ml, fl.]", Unit.MG),
            new Format("DINJVINC2V", "2 ml, vial [fr:2 ml, fl.]", Unit.MG)
        ),
        new Drug("DINJWATE", "WATER for injection [fr:EAU pour injection]").withFormats(
            new Format("DINJWATE1A", "10 ml, plastic amp. [fr:10 ml, amp. plastique]", Unit.MG)
        )
    );

    Category VACCINE = new Category("DVAC", "vaccines/immunoglobulins", DosingType.QUANTITY).withDrugs(
        new Drug("DVACDTUB", "TUBERCULIN, 5 TU/0.1 ml, multidose [fr:TUBERCULINE, 5 UI/0.1 ml, multidose]").withFormats(
            new Format("DVACDTUB5T", "1 dose, vial. [fr:1 dose, fl.]", Unit.ML)
        ),
        new Drug("DVACIMAS", "IMMUNOGLOBULIN AFRICAN [fr:IMMUNOGLOBULINE ANTIVENIN]").withFormats(
            new Format("DVACIMAS2V", "SNAKES ANTIVENOM EchiTab-Plus, vial [fr:SERPENTS AFRICAINS EchiTab-Plus,fl]", Unit.MG),
            new Format("DVACIMAS3A", "SNAKE ANTIVENOM, SAIMR, 10 ml amp. [fr:SERPENT AFRICAINS, SAIMR, 10 ml amp]", Unit.MG)
        ),
        new Drug("DVACIMHB", "IMMUNOGLOBULIN HUMAN HEPATITIS B, 180 IU/ ml [fr:IMMUNOGLOBULINE HUMAINE HEPATITE B, 180 UI/ ml]").withFormats(
            new Format("DVACIMHB1V", "1 ml, vial [fr:1 ml, fl.]", Unit.MG)
        ),
        new Drug("DVACIMHD", "IMMUNOGLOBULIN HUMAN anti-D, 300µg [fr:IMMUNOGLOBULINE HUMAINE anti-D, 300µg]").withFormats(
            new Format("DVACIMHD1S", "syringe [fr:seringue]", Unit.MG),
            new Format("DVACIMHD1V", "powder + diluent, vial [fr:poudre + solvant, fl.]", Unit.MG)
        ),
        new Drug("DVACIMHR", "IMMUNOGLOBULIN HUMAN ANTIRABIES [fr:IMMUNOGLOBULINE HUM.]").withFormats(
            new Format("DVACIMHR3V", "150 UI/ ml, 2 ml, vial [fr:ANTIRABIQUES, 150 UI/ ml, 2 ml, fl.]", Unit.MG),
            new Format("DVACIMHR3V1", "300IU/ ml, 1 ml, vial [fr:ANTIRABIQUE, 300UI/ ml, 1 ml, fl.]", Unit.MG),
            new Format("DVACIMHR3V5", "300IU/ ml, 5 ml, vial [fr:ANTIRABIQUE, 300UI/ ml, 5 ml, fl.]", Unit.MG)
        ),
        new Drug("DVACIMPH", "IMMUNOGLOBULIN, polyvalent, human, 0.1g/ ml [fr:IMMUNOGLOBULINE, polyvalent, humaine, 0.1g/ ml]").withFormats(
            new Format("DVACIMPH1V", "100 ml, vial [fr:100 ml, fl.]", Unit.MG)
        ),
        new Drug("DVACIMTE", "IMMUNOGLOBULIN HUMAN ANTITETANUS [fr:IMMUNOGLOBULINE HUM. ANTITETANIQUE]").withFormats(
            new Format("DVACIMTE2S", "250 IU/ ml, syr. [fr:250 UI/ ml, sering.]", Unit.MG)
        ),
        new Drug("DVACVBCG", "").withFormats(
            new Format("DVACVBCG3SD", "(vaccine BCG) DILUENT, 1 dose, multidose vial [fr:(vaccin BCG) SOLVANT, 1 dose, multidose fl.]", Unit.MG),
            new Format("DVACVBCG3VD", "VACCINE BCG, 1 dose, multidose vial, 0.05 ml/dose [fr:VACCIN BCG, 1 dose, fl. multidose, 0.05 ml/dose]", Unit.MG)
        ),
        new Drug("DVACVCHO", "VACCINE CHOLERA, ORAL, monodose, 1.5 ml [fr:VACCIN CHOLERA, ORAL, monodose, 1,5 ml]").withFormats(
            new Format("DVACVCHO1PT", "plastic tube [fr:tube plast.]", Unit.MG),
            new Format("DVACVCHO1V", "vial [fr:fl]", Unit.MG)
        ),
        new Drug("DVACVDHH", "VACCINE DPT / HEPATITIS B / Hib [fr:VACCIN DTC / HEPATITE B / Hib]").withFormats(
            new Format("DVACVDHH1VD", "1 dose, multidose vial [fr:1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVDTB", "VACCINE Td (tetanus/diphtheria booster)1 dose,multidose vial [fr:VACCIN Td (tétanos/diphtérie dose rappel) 1 dose, fl.multid.]").withFormats(
            new Format("DVACVDTB1VD", "", Unit.MG)
        ),
        new Drug("DVACVENC", "VACCINE JAPANESE ENCEPHALITIS, monodose, syringe [fr:VACCIN ENCEPHALITE JAPONAISE, monodose, seringue, 0]").withFormats(
            new Format("DVACVENC1S", "0.5 ml [fr:5 ml]", Unit.MG)
        ),
        new Drug("DVACVHEA", "VACCINE HEPATITIS A [fr:VACCIN HEPATITE A]").withFormats(
            new Format("DVACVHEA1S", "1 dose, adult, monodose, syringe [fr:1 dose, adult, monodose, seringue]", Unit.MG)
        ),
        new Drug("DVACVHEB", "VACCINE HEPATITIS B, 1 [fr:VACCIN HEPATITE B, 1 dose]").withFormats(
            new Format("DVACVHEB1U", "adult dose, monodose, uniject [fr:adulte, monodose, uniject]", Unit.MG),
            new Format("DVACVHEB1VD", "adult dose, multidose vial [fr:adulte, fl. multidose]", Unit.MG),
            new Format("DVACVHEB2V", "adult dose, monodose, 1 ml, vial [fr:adulte, monodose, 1 ml, fl.]", Unit.MG),
            new Format("DVACVHEB3V", "child dose, monodose, 0.5 ml, vial [fr:enfant, monodose, 0,5 ml, fl.]", Unit.MG),
            new Format("DVACVHEB3VD", "child dose, multidose vial [fr:enfant, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVHIB", "VACCINE HAEMOPHILUS INFLUENZAE type b, monodose [fr:VACCIN HAEMOPHILUS INFLUENZAE type b, monodose, 0]").withFormats(
            new Format("DVACVHIB1S", "0.5 ml,syr. [fr:5 ml, ser.]", Unit.MG)
        ),
        new Drug("DVACVHPV", "VACCINE HPV [fr:VACCIN HPV]").withFormats(
            new Format("DVACVHPV2V", "bivalent, monodose, 0.5 ml, vial [fr:bivalent, monodose, 0,5 ml, fl.]", Unit.MG),
            new Format("DVACVHPV4V", "quadrivalent, monodose, 0.5 ml, vial [fr:quadrivalent, monodose, 0,5 ml, fl.]", Unit.MG)
        ),
        new Drug("DVACVMEA", "").withFormats(
            new Format("DVACVMEA2SD", "(vaccine measles) DILUENT, 1 dose, multidose vial [fr:(vaccin rougeole) SOLVANT, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVMEA2VD", "VACCINE MEASLES, 1 dose, multidose vial [fr:VACCIN ROUGEOLE, 1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVMEN", "").withFormats(
            new Format("DVACVMEN1VWCJ", "VACCINE MENINGITIS CJ A+C+W135+Y, monod.+ dil.0.5 ml (Menveo) [fr:VACCIN MENINGITE CJ A+C+W135+Y, monod.+ solv.0,5 ml (Menveo)]", Unit.MG),
            new Format("DVACVMEN2VWCJ", "VACCINE MENINGITIS CJ A+C+W135+Y, monod.,vial (Menactra) [fr:VACCIN MENINGITE CJ A+C+W135+Y, monod. fl. (Menactra)]", Unit.MG),
            new Format("DVACVMEN3VWCJ", "VACCINE MENINGITIS CJ A+C+W135+Y,monod.+dil.0.5 ml (Nimenrix) [fr:VACCIN MENINGITE CJ A+C+W135+Y, monod.+solv.0.5 ml (Nimenrix)]", Unit.MG),
            new Format("DVACVMENA1SD", "(vaccine mening. A conj. 1-29y) DILUENT 1 dose, multidose v. [fr:(vaccin méning. A conj. 1-29ans) SOLVANT 1 dose,fl.multidose]", Unit.MG),
            new Format("DVACVMENA1VD", "VACCINE MENINGOCOCCAL A CONJUGATE, 1-29years, 1dose,multid.v [fr:VACCIN MENINGOCOQUE A CONJUGUE, 1-29 ans, 1dose, fl. multid.]", Unit.MG),
            new Format("DVACVMENA2SD", "(vaccine mening. A conj. 3-24m) DILUENT, 1 dose, multidose [fr:(vaccin méning. A conj. 3-24mois) SOLVANT, 1 dose, multidose]", Unit.MG),
            new Format("DVACVMENA2VD", "VACCINE MENINGOCOCCAL A CONJ. 3-24months, 1dose, multid.vial [fr:VACCIN MENINGOCOQUE A CONJ. 3-24 mois, 1dose, fl. multid.]", Unit.MG)
        ),
        new Drug("DVACVMER", "").withFormats(
            new Format("DVACVMER1SD", "(MEASLES/RUBELLA VACCINE) Diluent multidose, 1 dose, bottle [fr:(VACCIN ROUGEOLE/ROUBEOLE) Solvant multidose, 1 dose, fl.]", Unit.MG),
            new Format("DVACVMER1VD", "MEASLES/RUBELLA VACCINE, multidose, 1 dose, vial [fr:VACCIN ROUGEOLE/ RUBEOLE, multidose, 1 dose, fl.]", Unit.MG)
        ),
        new Drug("DVACVMMR", "").withFormats(
            new Format("DVACVMMR1SD", "(vaccine MMR) DILUENT, 1 dose, multidose vial [fr:(vaccin ROR) SOLVANT, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVMMR1VD", "VACCINE MMR (measles, mumps, rubella), 1 dose,multidose vial [fr:VACCIN ROR (rougeole,oreillons,rubéole), 1dose, fl.multidose]", Unit.MG),
            new Format("DVACVMMR2S", "(VACCINE MMR,measles/mumps/rubella) DILUENT, monodose, amp. [fr:(VACCIN ROR,rougeole/oreillons/rubéoleI) SOLVANT, monod amp.]", Unit.MG),
            new Format("DVACVMMR2V", "VACCINE MMR (measles/mumps/rubella), monodose vial [fr:VACCIN ROR (rougeole/oreillons/rubéole), monodose, fl.]", Unit.MG)
        ),
        new Drug("DVACVPCV", "VACCINE PNEUMOCOCCAL CONJUGATE [fr:VACCIN PNEUMOCOQUES CONJUGUE]").withFormats(
            new Format("DVACVPCV13VD", "PCV13, 1 dose,vial, multidose [fr:PCV13, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVPCV13VDN", "PCV13,1dose,multid.vl noGAVI [fr:PCV13, 1dose,fl.multid.nonGAVI]", Unit.MG),
            new Format("DVACVPCV13VDS", "PCV13, 1dose,multid.vial spec [fr:PCV13, 1dose, fl.multid. spéc.]", Unit.MG),
            new Format("DVACVPCV2VD", "PCV10, 1 dose,vial, multidose [fr:PCV10, 1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVPOI", "VACCINE POLIOMYELITIS, INACTIVATED [fr:VACCIN]").withFormats(
            new Format("DVACVPOI1VD", "1 dose, multidose vial [fr:POLIO, INACTIVE, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVPOI2S", "0.5 ml, monodose syringe [fr:POLIO INACTIVE (IPV) inject, monodose, 0,5 ml, sering]", Unit.MG)
        ),
        new Drug("DVACVPOL", "").withFormats(
            new Format("DVACVPOL13BD", "VACCINE POLIOMYELITIS, BIVALENT ORAL,1 dose, multidose vial [fr:VACCIN POLIO, BIVALENT ORAL, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVPOL13DR", "(bivalent oral polio vaccine) DROPPER [fr:(vaccin polio oral bivalent) COMPTE-GOUTTE]", Unit.MG)
        ),
        new Drug("DVACVPPV", "VACCINE PNEUMOCOCCAL polysaccharide 23, monodose [fr:VACCIN PNEUMOCOQUE polysaccharide 23, monodose]").withFormats(
            new Format("DVACVPPV23S", "0.5 ml,syr. [fr:0.5 ml, ser.]", Unit.MG)
        ),
        new Drug("DVACVRAB", "VACCINE RABIES, CCV, cell culture, monodose [fr:VACCIN]").withFormats(
            new Format("DVACVRAB1V", "vial [fr:ANTIRABIQUE, VCC, culture cellulaire, monodose, fl.]", Unit.MG),
            new Format("DVACVRAB2S", "syringe [fr:ANTIRABIQUE, VCC, culture cellulaire, monodose, ser.]", Unit.MG),
            new Format("DVACVRAB3V", "vial [fr:ANTIRABIQUE,VCC,culture cellulaire,monodose, fl.]", Unit.MG)
        ),
        new Drug("DVACVROT", "VACCINE ROTAVIRUS, ORAL (Rotarix), monodose, 1 [fr:VACCIN ROTAVIRUS, ORAL (Rotarix), monodose, 1]").withFormats(
            new Format("DVACVROT1T", "5 ml, tube [fr:5 ml, tube]", Unit.MG)
        ),
        new Drug("DVACVTET", "VACCINE TT (tetanus) [fr:VACCIN TT (tétanos)]").withFormats(
            new Format("DVACVTET1S", "monodose, 0.5 ml, syringe [fr:monodose, 0,5 ml, seringue]", Unit.MG),
            new Format("DVACVTET1VD", "1 dose, multidose vial [fr:1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVTYP", "VACCINE [fr:VACCIN TYPHOIDIQUE]").withFormats(
            new Format("DVACVTYP1S", "TYPHOID polysaccharide 25µg, monodosis,0.5 ml,syringe [fr:polyosidique 25µg,monodose,0,5 ml,seringue]", Unit.MG),
            new Format("DVACVTYP2VD", "TYPHOID, \"Typhim Vi\", 1 dose, multidose vial [fr:\"Typhim Vi\", 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVTYPC1VD", "TYPHOID CONJUGATE,1dose, multidose vial [fr:CONJUGUE, 1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVYEF", "").withFormats(
            new Format("DVACVYEF1S", "VACCINE YELLOW FEVER, monodose amp. + syr. solvent 0.5 ml [fr:VACCIN FIEVRE JAUNE, monodose amp. + seringue solvant 0,5 ml]", Unit.MG),
            new Format("DVACVYEF2SD", "(vaccine yellow fever) DILUENT, 1 dose, multidose vial [fr:(vaccin fièvre jaune) SOLVANT, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVYEF2VD", "VACCINE YELLOW FEVER, 1 dose, multidose vial [fr:VACCIN FIEVRE JAUNE, 1 dose, fl. multidose]", Unit.MG)
        )
    );

    CatalogIndex INDEX = new CatalogIndex(ORAL, INJECTABLE, INFUSIBLE, EXTERNAL, VACCINE)
        .withRoutes(PO, IV, SC, IM)
        .withUnits(Unit.TABLET, Unit.CAPSULE, Unit.MG, Unit.ML, Unit.DROP, Unit.PUFF, Unit.AMPOULE, Unit.SACHET);
}
