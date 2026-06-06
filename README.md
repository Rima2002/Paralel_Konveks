Paralel Konveks Çokgen Kontrolü — Java performans ölçümü ve Amdahl hesaplaması

Bu GitHub deposu, rastgele nokta dizisinden oluşturulan bir çokgenin konveks olup olmadığını paralel olarak test eden bir Java programı içerir. Program aynı zamanda farklı iş parçacığı sayıları için çalışma süresini ölçer, hızlanmayı (speedup) hesaplar ve Amdahl yasası için seri bölüm oranını gösterir.

Açıklama
- Bu program bir nokta dizisinin (x,y) koordinatlarına göre çokgenin konveks olup olmadığını paralel olarak kontrol eder.
- Farklı iş parçacığı sayıları için çalışma süresi ölçülür, hızlanma (speedup) hesaplanır ve Amdahl yasası için seri bölüm (F) tahmini gösterilir.

Derleme
Windows (cmd) ile UTF-8 kod sayfası ayarlanarak derleyin:

```cmd
chcp 65001
javac -encoding UTF-8 ConvexPolygonParallel.java
```

Çalıştırma
Temel kullanım (varsayılan değerler program içinde bulunur):

```cmd
java -Dfile.encoding=UTF-8 ConvexPolygonParallel
```

Argümanlarla kullanım (sıralı):
- `pointCount` : oluşturulacak nokta sayısı (ör. 100000)
- `runs` : her yapılandırma için ortalama alınacak tekrar sayısı (ör. 3)
- `threadCounts` : virgülle ayrılmış iş parçacığı sayıları (ör. `1,2,4,8`)
- opsiyonel `--csv=dosya.csv` : sonuçları CSV dosyasına yazar

Örnek:

```cmd
java -Dfile.encoding=UTF-8 ConvexPolygonParallel 100000 5 1,2,4,8 --csv=sonuclar.csv
```

Çıktı
- Konsolda hizalanmış tablo halinde sonuç ve Amdahl tahmini gösterilir.
- Eğer `--csv` belirtildi ise `threads,ms,speedup,F` başlıklı bir CSV dosyası üretilir.

Notlar ve öneriler
- Daha kararlı ölçümler için `runs` değerini artırın (ör. 10 veya 20).
- `pointCount` büyüdükçe paralel iş parçacıklarının faydası daha belirgin olur.
- Kaynak kodda Türkçe yorumlar ve açıklamalar bulunmaktadır (`ConvexPolygonParallel.java`).

Hazır teslim için yapılmış değişiklikler
- `convertToPoints` dönüşümü benchmark dışında tek seferde yapılır (adil karşılaştırma).
- `isConvexPolygon` overloadları eklendi: `int[]` ve `List<Point>` için.
- Komut satırı argümanları eklendi: `pointCount`, `runs`, `threadCounts`, `--csv`.
- CSV çıktı seçeneği eklendi.
- Tüm kullanıcı görünen metinler Türkçeleştirildi.

İletişim
- İsterseniz ben kaynak kodu ZIP'leyip teslim için paketleyebilirim veya rapor (ör. kısa PDF) için otomatik bir CSV özet raporu oluşturabilirim.
