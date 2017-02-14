---
title: Latent Dirichlet Allocation
---

In natural language processing, latent Dirichlet allocation (LDA) is a generative statistical model that allows sets of observations to be explained by unobserved groups that explain why some parts of the data are similar. For example, if observations are words collected into documents, it posits that each document is a mixture of a small number of topics and that each word's creation is attributable to one of the document's topics.

# Topics

In LDA, each document may be viewed as a mixture of various topics. Where each document is considered to have a set of topics that are assigned to it via LDA. In LDA the topic distribution is assumed to have a Dirichlet prior. In practice, this results in more reasonable mixtures of topics in a document.

# Methods

Harp provides two methods of calculating LDA models. In the following links, you can find the details in using these methods.

* [CVB LDA](/docs/examples/cvblda/)