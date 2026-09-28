# CraftArena: Block Legends (5vs5 Minecraft MOBA in Java)

Game MOBA 5vs5 bertema voxel / blok ala Minecraft yang dibuat menggunakan bahasa pemrograman Java murni (Standard Edition, tanpa dependensi eksternal).

---

## 🌟 Fitur Baru (Update 3.5 - Jungle Buffs, Turtle & Lord Evolution, Clumsy Roam/Jungler AI, & Objective HUD)

1. **Monster Buff Jungle (Blue Buff & Red Buff)**:
   - **Lapis Golem (Blue Buff)**:
     - Monster golem berbadan lapis lazuli dengan kepalan kristal cyan bercahaya. Bersemayam di hutan atas kedua tim.
     - Memberikan **Blue Buff (75 detik)**: Regenerasi Mana kilat (+18 MP/s) dan Cooldown Reduction (+20% CDR pada semua skill).
   - **Magma Fiend (Red Buff)**:
     - Monster magma berapi dengan tanduk membara dan mata kuning menyala. Bersemayam di hutan bawah kedua tim.
     - Memberikan **Red Buff (75 detik)**: Setiap *basic attack* memberikan bonus **+25 True Damage Burn** (mengabaikan armor musuh) serta partikel letupan api oranye.
   - **Visual Lingkaran Rune Hero (Orbiting Runes)**:
     - Hero yang memiliki buff aktif memiliki visual cincin rune voxel berputar di sekeliling kakinya (Rune cyan untuk Blue Buff, Rune emas/oranye untuk Red Buff).
   - **Indikator Buff pada HUD & Minimap**:
     - HUD konsol bawah menampilkan sisa durasi buff aktif secara real-time: `[Blue Buff: 72s]` dan `[Red Buff: 75s]`.
     - Minimap menampilkan ikon kotak cyan (Blue Buff) dan kotak oranye-merah (Red Buff) yang hilang saat monster mati dan muncul kembali saat respawn (75s).

2. **Sistem Waktu Spawn & Evolusi Dinamis Turtle & Lord**:
   - **Menit 03:00 (180 detik)**: Dragon Turtle pertama kali muncul (*spawn*) di river pit.
   - **Menit 06:00 (360 detik)**: Dragon Turtle kedua muncul / respawn jika telah terbunuh sebelumnya.
   - **Menit 08:00 (480 detik) - Sistem Transisi & Evolusi Cerdas**:
     - **Kondisi Tanpa Serangan**: Jika pada menit ke-8 Turtle tidak disentuh/diserang oleh siapa pun, ia langsung bertransformasi menjadi **Nether Lord**.
     - **Kondisi Sedang Diserang**: Jika Turtle sedang aktif diserang pada menit ke-8, ia **TIDAK AKAN** berubah menjadi Lord selama pertempuran masih berlangsung.
     - **Kondisi Diserang Sekali / Lepas Kontak**: Jika Turtle sempat terkena serangan (misal dicolek sekali) lalu musuh kabur / berhenti menyerang, tepat **30 detik** setelah pertempuran usai, Turtle akan bertransformasi menjadi Nether Lord!
   - **Menit 10:00 (600 detik)**: Jika Turtle telah mati terbunuh sebelumnya, maka Nether Lord akan muncul tepat pada menit ke-10.
   - **Live Objective Timer pada Scoreboard**:
     - Banner atas menampilkan hitungan mundur objektif secara langsung (misal: `Turtle: 02:45`, `Lord: 01:30`, `Turtle Active!`, atau `Lord Active!`).

3. **Perilaku Dinamis AI Roam (Kawal Gold Lane & Rotasi) & AI Jungler (Ambil Buff & Boss)**:
   - **Roam AI Mengawal Gold Lane & Rotasi Fleksibel**:
     - Hero Roamer (Iron Golem / Warden) secara konsisten mendampingi dan mengawal **Gold Laner** (Marksman / Alex / Wither Skeletor) di jalurnya untuk menahan serangan (*body blocking* dan peel).
     - Memiliki siklus rotasi dinamis ke **Mid Lane** dan **EXP Lane** untuk membantu inisiasi ganking dan membuka vision semak, lalu kembali mengawal Gold Lane.
     - Di sekitar Turtle/Lord yang aktif, Roamer tetap apatis ("ga kompak") menjauh dari pit boss, membiarkan Jungler bertarung sendiri.
   - **Jungler AI Mengambil Buff & Berusaha Kontes Turtle/Lord**:
     - AI Jungler (Nether Knight / Shadow Creeper) secara aktif bergerak ke camp monster **Blue Buff** dan **Red Buff** saat monster hutan hidup, menumbangkannya menggunakan skill dan basic attack untuk mengantongi buff!
     - **Prioritas Penuh Objektif Turtle / Lord**: Ketika Dragon Turtle atau Nether Lord aktif di sungai, Jungler yang berdarah sehat ($\ge 60\%$ HP) akan langsung bergegas dari posisi mana pun di peta menuju ceruk sungai (*boss pit*), mengunci target dan melancarkan kombo serangan untuk merebut objektif tim!
     - Jika HP Jungler turun di bawah 60% saat melawan boss, ia panik lari ke base (*goblok*) dan kembali lagi setelah darah pulih.
    - **Pemberantasan Total Bug Maju-Mundur di Perbatasan Turret & Deep Safe Retreat**:
      - **Mundur Jauh Saat Tidak Ada Minion ("Mundur Nunggu Minion")**: Mengatasi tuntas bug di mana bot maju-mundur atau bertahan terlalu dekat di perbatasan turret saat minion habis. Jika tidak ada minion kawan yang menahan turret (`alliedMinionsNear == 0`), bot langsung **mundur jauh ke perimeter aman** (`safeTowerDist = 11.0f`), keluar dari jangkauan poke dan tembakan turret untuk menunggu gelombang minion berikutnya.
      - **Zero Maju-Mundur / Anti-Jitter**: Bot menolak mengejar musuh atau minion yang berada di dalam/dekat perimeter turret tanpa minion. Waypoint marching terkunci di batas aman 11.0 blok hingga minion kawan benar-benar tiba di bawah turret.
    - **Minion Sepenuhnya Buta Terhadap Hero di Rumput (Minion Bush Blindness)**:
      - Minion tidak memiliki vision ke dalam semak/rumput (`isInBush`). Minion musuh **tidak akan pernah melihat, menargetkan, atau mengejar** hero kawan/pemain yang bersembunyi di semak selama hero tidak aktif menyerang (`revealTimer <= 0`).
      - Jika hero berlari masuk ke semak saat dikejar minion, minion langsung kehilangan aggro/target (`targetEntity = null`) dan kembali berjalan di jalurnya.
      - Tembakan atau damage dari minion dan turret tidak akan membatalkan status kamuflase semak hero (hanya damage hero lawan yang membuka kamuflase).
      - Radius deteksi semak diperluas secara presisi ke fringe (0.70 blok) sehingga pemain yang berdiri di tepi rumput tetap tersembunyi dengan sempurna.
     - **Motion-Verified WalkAnim & Zero-Jitter Escort**: Logika kalkulasi pergerakan hero memverifikasi perubahan posisi nyata sebelum memutar animasi langkah kaki walkAnim. Saat hero diam atau mencapai perimeter kawan/turret, animasi langkah kaki otomatis di-reset ke 0.
     - Pengawalan Roamer menerapkan *distance hysteresis* (kejar jika jarak $> 3.8$, berdiri tegak berjaga jika jarak $\le 3.8$) sehingga tidak terjadi osilasi bolak-balik.
   - **Minion Jalur Kebal Distraksi Monster Hutan (Target Filtering)**:
    - **Taktik Makro AI ala Pro Bot MLBB Saat Minion Habis di Turret (Anti-Idle Macro Tactics)**:
      - **Recall Pulang ke Base Saat Darah Kritis / Terluka (< 60% HP)**: Alih-alih berdiam diri di lane saat minion habis yang rentan di-gank, bot segera recall pulang ke spawn base untuk regenerasi HP dan belanja item.
      - **Sembunyi & Pasang Perangkap di Semak (Bush Ambush & Stealth Zoning)**: Jika HP sehat dan ada semak terdekat yang aman di luar jangkauan turret, bot bergerak masuk ke dalam rumpun semak (*tall grass*), mengaktifkan mode stealth tak terlihat di minimap, dan bersiap menyergap musuh yang lewat.
      - **Farming Monster Buff & Creep Hutan Terdekat**: Memanfaatkan jeda minion untuk membersihkan camp monster buff netral yang hidup di sekitar jalur demi menambah gold dan exp.
      - **Cegat & Sambut Gelombang Minion Lawan (Wave Interception)**: Proaktif bergerak menyambut gelombang minion lawan yang mendekat di luar jangkauan turret untuk segera di-clear dan di-last hit.
      - **Rotasi Gank Sungai & Jalur Lain**: Bot peran Mid dan Roam secara cerdas merotasikan posisi ke arah ceruk sungai (*river*) untuk mengintai objektif boss atau membantu gank kawan di jalur lain.
     - Minion jalur difilter secara ketat agar **TIDAK PERNAH** menyerang monster jungle netral (`BuffMonster` & `JungleCamp`). Minion murni berbaris di lane menyerang minion musuh, hero musuh, dan turret lawan.

4. **Sistem Atribusi Kill & Assist 20 Detik (20-Second Damage Attribution Rule)**:
   - **Aturan Eksekusi Turret, Minion, Buff, Turtle & Lord**:
     - Jika seorang hero tewas akibat tembakan Turret, serangan Minion, Monster Buff, Turtle, atau Lord:
       - Sistem secara otomatis memeriksa riwayat damage dalam **20 detik terakhir**.
       - Jika ada hero musuh yang memberikan damage ke korban dalam 20 detik tersebut, maka **KILL DIBERIKAN KEPADA HERO MUSUH TERSEBUT** (+1 kill score, +320 Gold reward, +260 XP, dan nama hero tercantum di kill banner & killfeed)!
       - Hanya jika **TIDAK ADA** hero musuh yang menyentuh korban dalam 20 detik terakhir, kematian korban dianggap murni eksekusi lingkungan (*executed by Turret / Minion / Neutral*).
   - **Atribusi Last-Hit Minion Jalur**:
     - Jika minion musuh tewas oleh tembakan turret kawan atau minion kawan, sistem mengecek apakah ada hero kawan yang memberikan damage dalam 20 detik terakhir. Jika ada, hero tersebut tetap mendapatkan kredit Creep Score (CS) dan reward emas!

---

## 🌟 Fitur Sebelumnya (Update 3.0 - Format Lengkap 5v5 MOBA, Turtle, Lord, Rumput Sembunyi, AI Cerdas, & Draft Pick)

1. **Format Lengkap 5v5 MOBA & 5 Role / Lane (10 Hero Voxel Unik)**:
   - **5 Role Standar MOBA**:
     - **EXP Lane**: Solo fighter tangguh, duel di jalur atas (Steve vs Piglin Brute).
     - **Mid Lane**: Burst mage dengan waveclear kilat dan rotasi cepat (Evoker vs Herobrine).
     - **Gold Lane**: Ranged marksman dengan penskalaan damage dahsyat (Alex vs Wither Skeletor).
     - **Roam**: Tank pelindung kawan, inisiasi teamfight dan penguasa vision semak (Iron Golem vs Warden).
     - **Jungle**: Pemburu monster hutan, eksekutor Turtle/Lord dan spesialis ambush culik (Nether Knight vs Shadow Creeper).
   - **4 Hero Baru Voxel 3D**:
     - **Iron Golem** (Blue Roam/Tank): HP super tebal, Iron Uppercut lempar musuh ke udara, Rose Shield, dan Seismic Quake area stun.
     - **Nether Knight** (Blue Jungle/Assassin): Sepasang pedang kirmizi, Flame Dagger perlambat musuh, Nether Shadow dash, dan Abyssal Execute loncatan mematikan.
     - **Warden** (Red Roam/Tank): Sculk Grasp penjerat kaki, Sonic Boom gelombang kejut tembus pandang, Dark Dash, dan Echoing Roar raungan getar.
     - **Shadow Creeper** (Red Jungle/Assassin): Bilah belati gank semak, Sulfur Smoke kabut racun pelambat, Shadow Stalk, dan Nitro Blast ledakan instan.

2. **Sistem Boss Sungai: Dragon Turtle & Nether Lord**:
   - **Dragon Turtle (Early to Mid Game)**:
     - Bersemayam di ceruk sungai pit, memiliki HP 2.800 dan voxel model tempurung kura-kura naga air dengan sirip mengapung.
     - Tim yang mengeliminasi Turtle mendapatkan **250 HP Turtle Shield** (perisai proteksi kawan), bonus emas (+160G), dan XP tim.
     - Status perisai ditampilkan jelas pada bar darah pemain: `[+250 Shield]` lengkap dengan visual border absorption.
   - **Nether Lord (Late Game Boss & Siege Colossus)**:
     - Setelah menit ke-6 atau saat pertempuran larut, Turtle berevolusi menjadi Nether Lord (HP 5.200, serangan ground tremor dahsyat).
     - Tim yang menumbangkan Lord akan mendapatkan berkah damage tim, dan memanggil **Summoned Lord** (Titan Voxel Berzirah Tanduk) yang berbaris bersama minion kawan menuju base musuh untuk merobohkan turret lawan!

3. **Sistem Rumput / Semak-Semak Stealth (Bisa Sembunyi & Culik)**:
   - 10 gugus semak rumput tinggi Minecraft (*3D tall grass* dengan animasi angin melambai) ditanam di titik-titik strategis sungai, pit boss, dan lane.
   - **Mekanisme Stealth Sempurna**: Hero musuh yang berada di dalam semak **sepenuhnya tidak terlihat di layar maupun di minimap**.
   - Musuh hanya terungkap jika:
     1. Hero kawan melangkah masuk ke dalam rumpun semak yang sama, ATAU
     2. Hero kawan berada pada jarak kontak point-blank ($\le 1.4$ blok), ATAU
     3. Musuh menyerang / terkena serangan (terbuka selama 2.5 detik).

4. **Kecerdasan Buatan (Smart AI) Cerdas & Tidak Nafsu Kill**:
   - **Tidak Nafsu Kill Sembarangan (Anti-Suicide Turret Dive)**:
     - AI bot memperhitungkan keberadaan turret musuh dan gelombang minion sebelum mengejar target.
     - Jika target mundur ke dalam jangkauan turret dan tidak ada minion tim yang menahan tembakan (*tanking*), AI akan menahan diri dan mundur ke posisi aman alih-alih mati konyol di bawah turret.
   - **Pintar Culik di Rumput (Bush Ambush AI)**:
     - Hero Roamer dan Jungler secara mandiri bersembunyi di semak-semak sungai.
     - Ketika musuh yang lengah lewat di dekat semak, AI memicu kombo kejutan seketika: melesat keluar dengan Skill Dash/Blink lalu meluncurkan Ultimate dan serangan burst damage!

5. **Antarmuka Lengkap: Main Menu, Lane Select, Matchmaking, & Draft Pick**:
   - **Main Menu**: Tampilan pembuka bergaya voxel gelap dengan daftar fitur dan tombol `[ START 5v5 MATCH ]`.
   - **Lane / Role Select**: Pemilihan 5 kartu peran interaktif (EXP, MID, GOLD, ROAM, JUNGLE) lengkap dengan ikon emoji senjata dan deskripsi tugas.
   - **Matchmaking Screen**: Animasi putaran kompas voxel 8-arah mencari 10 pemain (`Players Found: 10 / 10`).
   - **Draft Pick Screen**: Hitungan mundur 15 detik MLBB, panel 5 hero tim Blue Alliance di kiri, 5 hero musuh Red Horde di kanan, kartu hero yang dapat dipilih, rincian skill hero terpilih, dan tombol `[ LOCK IN HERO ]`.

---

1. **Dedicated High-Precision Game Loop Thread (System.nanoTime)**:
   - Menggantikan `javax.swing.Timer` yang rentan jitter dan micro-stutter dengan thread loop game independen presisi nanodetik.
   - Menggunakan kalkulasi `dt` dinamis berdasarkan waktu nyata sehingga pergerakan hero, proyektil, dan animasi minion selalu mulus dan bebas dari efek *slow-motion* / lag.
   - Ditambahkan sinkronisasi native display buffer `Toolkit.getDefaultToolkit().sync()` agar tampilan monitor selalu tersegarkan secara konsisten (anti-stutter & anti-tearing).

2. **Akselerasi Perangkat Keras DirectX / Direct3D (Hardware Acceleration)**:
   - Startup script `run.bat` dilengkapi flag performa tinggi Java2D:
     `-Dsun.java2d.d3d=true -Dsun.java2d.noddraw=true -Dsun.java2d.transaccel=true -XX:+UseG1GC`
   - Memanfaatkan GPU untuk proses blitting sprite isometrik, efek transparansi (alpha compositing), dan rendering teks secara instan.

3. **Optimasi Rendering & Frustum Culling Ekstrem**:
   - **Ground Frustum Culling**: Blok tanah di luar batas viewport layar langsung dilewati sebelum proses pemanggilan draw, menghemat ratusan pemanggilan `drawImage` per frame.
   - **Zero Anonymous Allocations**: Menghilangkan alokasi objek anonim per frame pada pengecekan proyektil dan pohon.
   - **Pre-computed Turret Rings**: Lingkaran jangkauan turret dihitung dengan offset ellipse relatif yang sudah di-cache secara statis (tanpa perhitungan trigonometri `sin/cos` berulang di setiap frame).
   - **Cached Drawing Strokes & Fonts**: `BasicStroke` dan `Font` disimpan secara statis guna memangkas kerja Garbage Collector (GC).

4. **Kalkulasi Tabrakan & Koleksi Entitas Efisien**:
   - Pengecekan jarak tabrakan kini menggunakan *squared distance* ($dx^2 + dy^2 < r^2$) sebelum memanggil `Math.sqrt()`, menghemat ribuan operasi akar kuadrat per detik.
   - Mengganti array copy berat pada pembaruan proyektil & partikel dengan penghapusan terbalik $O(1)$.

---

## 🌟 Fitur Sebelumnya (Update 2.3 - Notifikasi Kill/Death ala MLBB & Respawn Cepat)

1. **Hitungan Mundur Respawn Cepat & Snappy ala MLBB**:
   - Durasi respawn diatur jauh lebih cepat dan dinamis: dimulai dari **3.0 detik** (+0.8 detik per level), bukan lagi puluhan detik yang lama.
   - Format tampilan menggunakan detik bulat yang tegas (**"RESPAWNING IN: 4s"**, **"3s"**, **"2s"**, **"1s"**) seperti di Mobile Legends (MLBB).
   - Kartu status hero di kiri dan kanan atas juga diperbarui dengan format detik bulat (**"Respawn: 4s"**).

2. **Banner Pengumuman Kill / Death ala MLBB (Top Center Banner)**:
   - **Saat Pemain Membunuh Musuh**:
     - Banner emas bercahaya: **"YOU HAVE SLAIN AN ENEMY!"**
     - Subtitle detail: `Steve  ⚔  Herobrine (+320 Gold)`
     - Efek audio level-up / kemenangan.
   - **Saat Pemain Dibunuh / Mati**:
     - Banner merah tua bercahaya: **"YOU HAVE BEEN SLAIN!"**
     - Subtitle detail: `Slain by Herobrine`
     - Efek audio ledakan / kekalahan.
   - **Saat Kawan Membunuh Musuh**:
     - Banner biru toska: **"AN ENEMY HAS BEEN SLAIN!"**
   - **Saat Musuh Membunuh Kawan**:
     - Banner oranye merah: **"AN ALLY HAS BEEN SLAIN!"**
   - **Saat Turret Hancur**:
     - Banner tim: **"TURRET DESTROYED! (+250G)"** atau **"TURRET LOST!"**

3. **Killfeed Real-Time (Top-Right Feed)**:
   - Deretan log eliminasi di sudut kanan atas dengan badge nama pembunuh (biru/merah), ikon pedang emas `⚔`, dan nama korban, yang otomatis memudar setelah beberapa detik.

---

## 🌟 Fitur Sebelumnya (Update 2.2 - Sistem Peringatan Cerdas Aggro Turret)

1. **Sistem Peringatan Cerdas Aggro Turret (Target-Specific Alert)**:
   - **Saat Turret Menyerang Minion**:
     - **TIDAK berkedip merah** dan **TIDAK memunculkan teks peringatan bahaya**.
     - Area perimeter turret berubah menjadi **cincin kuning/amber lembut** (*Minion Aggro Ring*), menandakan pemain aman untuk mendekati turret atau melakukan dive/push selama minion menahan tembakan turret.
   - **Saat Turret Menyerang Hero Kawan Lain (Alex/Evoker)**:
     - Area perimeter tetap dalam kondisi oranye normal tanpa memunculkan peringatan bahaya ke layar pemain.
   - **HANYA Saat Turret Mengunci / Menyerang Kita (Player Steve)**:
     - Area jangkauan serangan turret **seketika berkedip merah terang intensif** (*Red Danger Pulse*).
     - Laser penarget turret terkunci ke badan Steve.
     - Teks peringatan bahaya mencolok muncul di atas turret:
       ```
       !! DANGER: TURRET ATTACKING YOU !!
       ```
   - **Aggro Transfer Cerdas**: Jika pemain menyerang hero musuh di bawah lindungan turret musuh, turret akan seketika mengalihkan tembakan ke pemain dan memicu alarm bahaya.

2. **Fog of War & Vision Culling (Objek Jauh Disembunyikan)**:
   - **Kawan Satu Tim Selalu Terlihat**: Rekan tim Blue (Steve, Alex, Evoker, minion biru, dan turret biru) selalu terlihat di layar dan minimap.
   - **Culling Musuh & Monster di Luar Vision**:
     - Hero musuh (Piglin Brute, Wither Skeletor), minion musuh, dan turret musuh yang berada jauh dari unit kawan **tidak akan dirender di layar** dan **disembunyikan dari minimap** (masuk ke dalam *Fog of War*).
     - Objek musuh hanya muncul jika masuk ke dalam radius penglihatan (*Vision Radius*) salah satu unit kawan.
   - **Screen Frustum Culling**: Blok dan unit di luar viewport kamera tidak diproses demi performa optimal.

3. **Format Pertempuran 3 vs 3 (6 Hero Unik)**:
   - **Blue Team**: Steve (MID), Alex (TOP), Evoker (BOT).
   - **Red Team**: Herobrine (MID), Piglin Brute (TOP), Wither Skeletor (BOT).

4. **Peta 3 Jalur (Top, Mid, Bottom) & Sungai**:
   - Peta 50 x 50 blok voxel dengan 12 Turret, 2 Nexus Core, dan Iron Titan Boss di sungai.

5. **Layar Kematian & Penghitung Mundur Respawn ("YOU DIED")**:
   - Layar dialog kematian merah Minecraft dengan hitungan mundur detik respawn real-time: **"RESPAWNING IN: X.X SECONDS"**.
   - Kartu status semua hero di bagian atas menampilkan timer respawn merah saat gugur.

---

## 🎮 Kontrol Game

| Tombol / Mouse | Aksi |
|---|---|
| **Klik Kanan / Kiri** | Bergerak ke tanah ATAU target serang unit musuh |
| **Q** | Skill 1: Diamond Whirlwind (Area Slash) |
| **W** | Skill 2: TNT Toss (Lempar Bom TNT) |
| **E** | Skill 3: Ender Pearl (Teleportasi / Blink) |
| **R** | Ultimate: Titan Slam (Stun & Shockwave) |
| **B** | Recall (Kembali ke Base Fountain) |
| **P** | Buka / Tutup Toko Item (Minecraft Shop) |
| **Spasi** | Pusatkan kamera ke hero (atau Restart saat Game Over) |
| **F2** | Tangkap Screenshot gameplay |

---

## 🚀 Cara Menjalankan & Menguji

```powershell
# Jalankan Game 3vs3:
.\run.bat

# Jalankan Test Suite & Screenshot Generator:
.\test.bat
```

Screenshot tersimpan di folder `screenshots/`:
- `screenshots/screenshot_turret_targeting_minion.png` (Saat turret fokus serang minion: cincin kuning tenang, tanpa kedipan merah, tanpa peringatan)
- `screenshots/screenshot_turret_targeting_player.png` (Saat turret serang kita: kedip merah terang & peringatan `!! DANGER: TURRET ATTACKING YOU !!`)
- `screenshots/screenshot_gameplay.png` (Pertempuran 3vs3 dengan FoW culling)
- `screenshots/screenshot_death_screen.png` (Layar "YOU DIED!" dengan countdown detik respawn)
- `screenshots/screenshot_shop.png` (Toko Item)
