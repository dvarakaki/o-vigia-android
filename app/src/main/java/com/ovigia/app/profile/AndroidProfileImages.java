package com.ovigia.app.profile;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.Size;

import com.ovigia.app.auth.AccountStore.ImageKind;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Implementação real de {@link ProfileImages}: decodifica com {@link ImageDecoder}
 * (que já aplica a rotação EXIF), reduz para no máximo {@link #AVATAR_MAX_PX} /
 * {@link #BANNER_MAX_PX} no lado maior e grava JPEG em {@code files/profile_media}.
 * Assim uma foto de 12 MP da câmera vira algumas centenas de KB.
 */
public final class AndroidProfileImages implements ProfileImages {

    static final int AVATAR_MAX_PX = 512;
    static final int BANNER_MAX_PX = 1600;
    private static final int JPEG_QUALITY = 88;
    /** Imagens publicadas online vão dentro dos documentos: mais comprimidas. */
    private static final int SHARED_JPEG_QUALITY = 72;

    private final ContentResolver resolver;
    private final Supplier<File> directory;

    /** @param directory resolvido só no executor de I/O — obter a pasta de arquivos já é acesso a disco. */
    public AndroidProfileImages(ContentResolver resolver, Supplier<File> directory) {
        this.resolver = resolver;
        this.directory = directory;
    }

    @Override
    public String importImage(Uri source, ImageKind kind, String accountId) throws IOException {
        int maxPx = kind == ImageKind.AVATAR ? AVATAR_MAX_PX : BANNER_MAX_PX;
        Bitmap bitmap = decodeScaled(ImageDecoder.createSource(resolver, source), maxPx);

        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, jpeg)) {
                throw new IOException("Falha ao comprimir imagem");
            }
        } finally {
            bitmap.recycle();
        }
        return write(jpeg.toByteArray(), kind, accountId);
    }

    @Override
    public String saveShared(String base64, ImageKind kind, String accountId) throws IOException {
        if (base64 == null) throw new IOException("Imagem vazia");
        try {
            // Já vem como JPEG reduzido do servidor: só gravar.
            return write(Base64.getDecoder().decode(base64), kind, accountId);
        } catch (IllegalArgumentException e) {
            throw new IOException("Imagem ilegível", e);
        }
    }

    /** Grava os bytes com um nome novo, por arquivo temporário + rename. */
    private String write(byte[] jpeg, ImageKind kind, String accountId) throws IOException {
        File dir = directory.get();
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Não foi possível criar " + dir);
        String name = kind.name().toLowerCase(Locale.ROOT) + "_" + accountId + "_"
                + System.currentTimeMillis() + ".jpg";
        File target = new File(dir, name);
        File tmp = new File(dir, name + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            out.write(jpeg);
        }
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return name;
    }

    /** Decodifica reduzindo para no máximo {@code maxPx} no lado maior. */
    private static Bitmap decodeScaled(ImageDecoder.Source source, int maxPx) throws IOException {
        try {
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                Size size = info.getSize();
                int longest = Math.max(size.getWidth(), size.getHeight());
                if (longest > maxPx) {
                    float scale = (float) maxPx / longest;
                    decoder.setTargetSize(Math.max(1, Math.round(size.getWidth() * scale)),
                            Math.max(1, Math.round(size.getHeight() * scale)));
                }
                // Bitmap de hardware não pode ser comprimido.
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        } catch (RuntimeException e) {
            throw new IOException("Imagem ilegível", e);
        }
    }

    @Override
    public File file(String fileName) {
        return fileName == null ? null : new File(directory.get(), fileName);
    }

    @Override
    public String encodeForSharing(String fileName, int maxPx) {
        File file = file(fileName);
        if (file == null || !file.exists()) return null;
        try {
            Bitmap bitmap = decodeScaled(ImageDecoder.createSource(file), maxPx);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, SHARED_JPEG_QUALITY, out)) return null;
            } finally {
                bitmap.recycle();
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    @Override
    public void delete(String fileName) {
        File file = file(fileName);
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
