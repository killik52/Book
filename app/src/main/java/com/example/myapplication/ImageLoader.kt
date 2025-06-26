package com.example.myapplication

import android.content.Context
import android.widget.ImageView
import coil.ImageLoader
import coil.load
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation

/**
 * Classe utilitária para carregamento de imagens usando Coil
 */
object ImageLoader {
    
    /**
     * Carrega uma imagem com configurações padrão
     */
    fun loadImage(
        imageView: ImageView,
        url: String?,
        placeholder: Int? = null,
        error: Int? = null
    ) {
        if (url.isNullOrBlank()) {
            placeholder?.let { imageView.setImageResource(it) }
            return
        }
        
        imageView.load(url) {
            placeholder?.let { placeholder(it) }
            error?.let { error(it) }
            crossfade(true)
            crossfade(300)
        }
    }
    
    /**
     * Carrega uma imagem circular (para avatares, por exemplo)
     */
    fun loadCircularImage(
        imageView: ImageView,
        url: String?,
        placeholder: Int? = null,
        error: Int? = null
    ) {
        if (url.isNullOrBlank()) {
            placeholder?.let { imageView.setImageResource(it) }
            return
        }
        
        imageView.load(url) {
            placeholder?.let { placeholder(it) }
            error?.let { error(it) }
            transformations(CircleCropTransformation())
            crossfade(true)
            crossfade(300)
        }
    }
    
    /**
     * Carrega uma imagem com cantos arredondados
     */
    fun loadRoundedImage(
        imageView: ImageView,
        url: String?,
        cornerRadius: Float = 8f,
        placeholder: Int? = null,
        error: Int? = null
    ) {
        if (url.isNullOrBlank()) {
            placeholder?.let { imageView.setImageResource(it) }
            return
        }
        
        imageView.load(url) {
            placeholder?.let { placeholder(it) }
            error?.let { error(it) }
            transformations(RoundedCornersTransformation(cornerRadius))
            crossfade(true)
            crossfade(300)
        }
    }
    
    /**
     * Carrega uma imagem de um arquivo local
     */
    fun loadLocalImage(
        imageView: ImageView,
        filePath: String?,
        placeholder: Int? = null,
        error: Int? = null
    ) {
        if (filePath.isNullOrBlank()) {
            placeholder?.let { imageView.setImageResource(it) }
            return
        }
        
        imageView.load(filePath) {
            placeholder?.let { placeholder(it) }
            error?.let { error(it) }
            crossfade(true)
            crossfade(300)
        }
    }
    
    /**
     * Carrega uma imagem com cache personalizado
     */
    fun loadImageWithCache(
        imageView: ImageView,
        url: String?,
        placeholder: Int? = null,
        error: Int? = null,
        enableCache: Boolean = true
    ) {
        if (url.isNullOrBlank()) {
            placeholder?.let { imageView.setImageResource(it) }
            return
        }
        
        imageView.load(url) {
            placeholder?.let { placeholder(it) }
            error?.let { error(it) }
            crossfade(true)
            crossfade(300)
            
            if (!enableCache) {
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
            }
        }
    }
    
    /**
     * Limpa o cache de imagens
     */
    fun clearCache(context: Context) {
        val imageLoader = ImageLoader(context)
        imageLoader.diskCache?.clear()
        imageLoader.memoryCache?.clear()
    }
    
    /**
     * Pré-carrega uma imagem no cache
     */
    fun preloadImage(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()
        
        imageLoader.enqueue(request)
    }
} 