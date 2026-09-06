import requests

# --- URL de ton API ---
url = "http://localhost:5000/api/similarity/check"

# --- Texte complet à tester (tu peux y inclure abstract, intro, conclusion, etc.) ---
text_pfe = """
La récupération et la réutilisation de l’eau usée s’est avérée être une option réaliste pour couvrir le déficit en eau et les besoins croissants en cette dernière dans notre pays, mais aussi pour se conformer aux règlements relatifs au rejet des eaux usées, en vue de la protection de l’environnement et de la santé publique. Cette réutilisation n’est pas un nouveau concept. Avec l’augmentation de la demande en eau, liée à l’augmentation de la population et de l’amélioration de niveau de vie. La réutilisation de l’eau épurée acquiert un rôle croissant dans la planification et le développement des approvisionnements supplémentaires en eau. Ce présent rapport constitue une synthèse de mon stage de fin d’étude effectué au sein de l’Office National de l’Électricité et de l’Eau potable (ONEE) -Branche Eau dont le principal but est de réaliser le projet d’étude d’opportunité de réutilisation des eaux usées épurées pour l'arrosage des espaces verts, à partir de la station d'épuration d'Ifrane. Au début, une synthèse rappelant les cadres juridiques et institutionnels relatifs à la réutilisation des eaux usées épurées détaillant les procédures administratives et réglementaires existantes en matière de l’environnement. Puis, une description détaillée des différentes composantes de notre projet concernant la zone d’étude et du milieu accueillant de ce projet, notamment: la climatologie, les ressources hydriques et les ressources forestières. Ensuite, une méthodologie relative à la collecte des données requises avec des informations sur la station d’épuration d’Ifrane. En fin, des calculs sur les besoins en eau nécessaires pour l’arrosage des espaces verts ont été effectués ainsi que l’étude du coût d’investissement et d’exploitation du projet. Mots-clés : Station d’épuration, Réutilisation, Espaces verts, Eau usée épurée,
GENERALE Les ressources en eau constituent le moteur vital au cadre de vie des populations et de l’économie. Bien qu’une politique de l’eau soit partie exemplaire, les ressources en eau au Maroc deviendront encore plus rares si la gestion actuelle ainsi que la demande en eau se maintiennent. Un objectif stratégique important de la politique de l’eau dans ce pays est donc d’augmenter de manière efficiente l’utilisation de l’eau. Situé dans une zone climatique principalement aride à semi-aride, le Maroc fait face à un défi croissant de la rareté de l'eau. La demande en eau dépasse la disponibilité en eau, les nappes phréatiques étant épuisées principalement à des fins agricoles. Il fait face aussi à la limitation des ressources naturelles et à leur raréfaction sous l’effet du changement climatique. En plus, les ressources en eau existantes sont soumises à une véritable pollution générée par les volumes croissants des eaux usées. En effet, le rejet en milieu naturel d’eaux non traitées ou mal traitées, génère une pollution catastrophique pour la biodiversité et la qualité des ressources en eau. C’est pourquoi il est nécessaire de traiter les eaux usées, et de favoriser leur réutilisation, afin de préserver la santé publique et l’environnement. Parmi les 165 milliards de m3 d’eaux usées qui sont collectés et traités par an dans le monde, seuls 2% sont aujourd’hui réutilisés. [1] Les volumes annuels des rejets des eaux usées ont fortement augmenté au cours des trois dernières décennies. Ils sont passés de 48 millions à 600 millions de m3 entre 1960 et 2005 pour atteindre 700 millions en l'an 2010. Selon les prévisions, ces rejets continueront à croître rapidement pour atteindre 900 millions de m3 à l'horizon 2030. [1],

"""

# --- Corps de la requête ---
data = {"text": text_pfe}

# --- Envoi de la requête ---
response = requests.post(url, json=data)

# --- Affichage du résultat ---
print("Statut:", response.status_code)
if response.ok:
    res = response.json()
    print("Résultats similaires trouvés:", len(res.get("results", [])))
    print("\n--- Top résultats ---")
    for r in res.get("results", [])[:]:
        print(f"→ {r['author']} | Score: {r['similarity_score']:.3f} | Risque: {r['risk_level']}")
        print(f"  {r['pdf_url']}\n")
else:
    print(response.text)
