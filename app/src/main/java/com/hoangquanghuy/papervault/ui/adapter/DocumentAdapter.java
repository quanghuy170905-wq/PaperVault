package com.hoangquanghuy.papervault.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hoangquanghuy.papervault.R;
import com.hoangquanghuy.papervault.data.local.DocumentEntity;

import java.util.List;

public class DocumentAdapter
        extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    public interface OnDocumentClickListener {
        void onDocumentClick(DocumentEntity document);
    }

    private final List<DocumentEntity> documentList;
    private final OnDocumentClickListener listener;

    public DocumentAdapter(
            List<DocumentEntity> documentList,
            OnDocumentClickListener listener
    ) {
        this.documentList = documentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);

        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DocumentViewHolder holder,
            int position
    ) {
        DocumentEntity document = documentList.get(position);

        holder.txtTitle.setText(document.getTitle());
        holder.txtCategory.setText(document.getCategory());
        holder.txtMimeType.setText(document.getMimeType());

        holder.itemView.setOnClickListener(view -> {
            listener.onDocumentClick(document);
        });
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle;
        TextView txtCategory;
        TextView txtMimeType;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTitle = itemView.findViewById(R.id.txtDocumentTitle);
            txtCategory = itemView.findViewById(R.id.txtDocumentCategory);
            txtMimeType = itemView.findViewById(R.id.txtDocumentMimeType);
        }
    }
}