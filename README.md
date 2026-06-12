# Paralel Konveks Kontrol Projesi

Bu proje, verilen bir çokgenin konveks olup olmadığını Java thread'leri ile paralel olarak kontrol eder. Program ayrıca farklı thread sayıları için çalışma süresini ölçer, hızlanma değerini hesaplar ve Amdahl yasasına göre seri bölüm oranı tahmini üretir.

## İçerik

- `Main.java`: Paralel konvekslik kontrolü, kullanıcı girişi ve benchmark kodu.
- `README.md`: Derleme, çalıştırma ve proje açıklamaları.
[Watch the demo video](https://www.youtube.com/watch?v=l_IC5yJNLsQ)
## Gereksinimler

- Java JDK 8 veya üzeri
- Windows cmd, PowerShell veya herhangi bir terminal

Java kurulumunu kontrol etmek için:

```cmd
javac -version
java -version
```

## Derleme

Windows cmd veya PowerShell içinde proje klasöründe şu komutu çalıştırın:

```cmd
javac -encoding UTF-8 Main.java
```

Derleme başarılı olursa `Main.class` ve iç sınıflara ait `.class` dosyaları oluşur.

## PROGRAMIN ÇALIŞTIRILMASI

Programı derledikten sonra aynı klasörde şu komutu çalıştırın:

```cmd
java Main
```

Program önce kullanıcıdan çokgen noktalarını ister. Noktalar `x y` veya `x,y` biçiminde girilebilir.

Örnek giriş:

```text
Nokta sayisi : 4
1. nokta : 0 0
2. nokta : 1 0
3. nokta : 1 1
4. nokta : 0 1
```

Bu örnek kare olduğu için sonuç `CONVEX` olur.

## Benchmark Argümanları

Program kullanıcı girişinden sonra otomatik olarak Amdahl benchmark'ı çalıştırır. Varsayılan değerler:

- `pointCount`: `500000`
- `runs`: `5`
- `threadCounts`: `1,2,4,8`

Bu değerler komut satırından değiştirilebilir:

```cmd
java Main <pointCount> <runs> <threadCounts> [--csv=dosya.csv]
```

Örnek:

```cmd
java Main 100000 5 1,2,4,8 --csv=sonuclar.csv
```

Bu komut:

- 100000 noktalı yapay bir konveks çokgen oluşturur.
- Her thread sayısı için 5 tekrar yapar.
- `1`, `2`, `4` ve `8` thread için süre ve hızlanma değerlerini hesaplar.
- Sonuçları `sonuclar.csv` dosyasına yazar.

## Algoritma Özeti

Konvekslik kontrolü, ardışık üç noktanın cross product işaretine bakılarak yapılır. Bir çokgen konvekstir diyebilmek için tüm sıfır olmayan dönüş işaretlerinin aynı yönde olması gerekir.

Paralel çalışmada:

1. Noktalar thread sayısına göre parçalara ayrılır.
2. Her thread kendi aralığındaki dönüş işaretlerini kontrol eder.
3. Bir thread konveks olmayan durum bulursa ortak durdurma sinyalini aktif eder.
4. Ana thread, tüm thread sonuçlarını birleştirerek nihai kararı verir.

## Çıktı

Program iki bölüm halinde çıktı verir:

1. Kullanıcının girdiği çokgen için konvekslik sonucu.
2. Amdahl benchmark tablosu.

Örnek tablo:

```text
      Thread |    Sure (ms) | Hizlanma |    Tahmini F
-------------+--------------+----------+-------------
           1 |        12.34 |     1.00 |       0.0000
           2 |         7.10 |     1.74 |       0.1494
```

## Notlar

- `Main.java` dosya adı değiştirilmemelidir; çünkü kaynak kodda `public class Main` tanımlıdır.
- Daha kararlı benchmark sonuçları için `runs` değeri artırılabilir.
- Çok büyük `pointCount` değerleri daha anlamlı paralel performans ölçümü sağlar.
