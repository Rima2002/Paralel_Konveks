# Paralel Konveks Kontrol Projesi

Bu proje, verilen bir cokgenin konveks olup olmadigini Java thread'leri ile paralel olarak kontrol eder. Program ayrica farkli thread sayilari icin calisma suresini olcer, hizlanma degerini hesaplar ve Amdahl yasasina gore seri bolum orani tahmini uretir.

## Icerik

- `Main.java`: Paralel konvekslik kontrolu, kullanici girisi ve benchmark kodu.
- `README.md`: Derleme, calistirma ve proje aciklamalari.

## Gereksinimler

- Java JDK 8 veya uzeri
- Windows cmd, PowerShell veya herhangi bir terminal

Java kurulumunu kontrol etmek icin:

```cmd
javac -version
java -version
```

## Derleme

Windows cmd veya PowerShell icinde proje klasorunde su komutu calistirin:

```cmd
javac -encoding UTF-8 Main.java
```

Derleme basarili olursa `Main.class` ve ic siniflara ait `.class` dosyalari olusur.

## Calistirma

```cmd
java Main
```

Program once kullanicidan cokgen noktalarini ister. Noktalar `x y` veya `x,y` biciminde girilebilir.

Ornek giris:

```text
Nokta sayisi : 4
1. nokta : 0 0
2. nokta : 1 0
3. nokta : 1 1
4. nokta : 0 1
```

Bu ornek kare oldugu icin sonuc `CONVEX` olur.

## Benchmark Argumanlari

Program kullanici girisinden sonra otomatik olarak Amdahl benchmark'i calistirir. Varsayilan degerler:

- `pointCount`: `500000`
- `runs`: `5`
- `threadCounts`: `1,2,4,8`

Bu degerler komut satirindan degistirilebilir:

```cmd
java Main <pointCount> <runs> <threadCounts> [--csv=dosya.csv]
```

Ornek:

```cmd
java Main 100000 5 1,2,4,8 --csv=sonuclar.csv
```

Bu komut:

- 100000 noktali yapay bir konveks cokgen olusturur.
- Her thread sayisi icin 5 tekrar yapar.
- `1`, `2`, `4` ve `8` thread icin sure ve hizlanma degerlerini hesaplar.
- Sonuclari `sonuclar.csv` dosyasina yazar.

## Algoritma Ozeti

Konvekslik kontrolu, ardisik uc noktanin cross product isaretine bakilarak yapilir. Bir cokgen konvekstir diyebilmek icin tum sifir olmayan donus isaretlerinin ayni yonde olmasi gerekir.

Paralel calismada:

1. Noktalar thread sayisina gore parcalara ayrilir.
2. Her thread kendi araligindaki donus isaretlerini kontrol eder.
3. Bir thread konveks olmayan durum bulursa ortak durdurma sinyalini aktif eder.
4. Ana thread, tum thread sonuclarini birlestirerek nihai karari verir.

## Cikti

Program iki bolum halinde cikti verir:

1. Kullanicinin girdigi cokgen icin konvekslik sonucu.
2. Amdahl benchmark tablosu.

Ornek tablo:

```text
      Thread |    Sure (ms) | Hizlanma |    Tahmini F
-------------+--------------+----------+-------------
           1 |        12.34 |     1.00 |       0.0000
           2 |         7.10 |     1.74 |       0.1494
```

## Notlar

- `Main.java` dosya adi degistirilmemelidir; cunku kaynak kodda `public class Main` tanimlidir.
- Daha kararli benchmark sonuclari icin `runs` degeri artirilabilir.
- Cok buyuk `pointCount` degerleri daha anlamli paralel performans olcumu saglar.
