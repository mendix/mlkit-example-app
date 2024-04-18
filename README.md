# Mendix ML Kit Demo

Here you can get the resources for start creating Smart Apps with [MLKit](https://docs.mendix.com/refguide/machine-learning-kit/)
Happy Hacking!

## Getting Started

This app is available in a GitHub repository, therefore you need to enable the [GitHub support](https://docs.mendix.com/refguide/on-premises-git/#preparing-git-support) for Studio Pro First.

Just create a new Mendix App, go to 'Download from Version Control Server', choose 'private server', add this repository git address and there you go!

Please make sure that the bertsquad-12-int8.onnx file is present in the /path/to/your/app/mlsource/bert folder. You can download it from [here](https://github.com/onnx/models/blob/main/validated/text/machine_comprehension/bert-squad/model/bertsquad-12-int8.onnx) if needed.

### Note for Parallels Users
This demo is packed with some computer vision libaries that may require a Virtual Machine restart or two before they are properly recognized

### Avaliable Demos and models

These are the models we implemented, when required, including the pre and post processors (remember these are java actions and you need to open the App Folder and check the javasource folder). Please feel free to review, dissect and ultimately re-use any of the components, engineering patterns and implementations you see below, and especially, learn and have fun!

#### Shallow Models

##### Clasification : Iris Species

Here you can find examples for solving the standard [Iris species classification problem](https://www.kaggle.com/datasets/uciml/iris). We provide a decision tree example, a random forest, a logistic regressor and an Ensemble demo for combining all of these should you want to reuse any of the [ML design patterns](https://www.oreilly.com/library/view/machine-learning-design/9781098115777/). 

##### Clasification : Content filtering

There we implemented a [Spam filter](https://www.kaggle.com/code/flywithcode/simple-spam-filter) using a TF/IDF Vectorizer 

##### Clasification : Titanic survivors

In this module you can find an example of solving the [titanic challenge dataset](https://www.kaggle.com/c/titanic) with a [SKLearn pipeline](http://scikit-learn.org/stable/modules/generated/sklearn.pipeline.Pipeline.html) that handles missing features and applies normalization, along a [Xgboost classifier](https://xgboost.readthedocs.io/).

#### Deep learning Models

Of course you can run neural networks in Mendix! Please see our examples below, both for implementation and especially, for pre/post processors.

#### Computer Vision

##### Image Classification : ResNet50

Find here a standard ResNet50 example fully implemented. Please refer to the [ONNX Model Zoo for details](https://github.com/onnx/models/tree/main/validated/vision/classification/resnet) on model and training

##### Style Transfer: Fast Neural Style

Yes, you can make an app that turns any picture into a Vermeer with Mendix. Here we show you how can you apply a Mosaic style transfer from [this](https://github.com/onnx/models/tree/main/validated/vision/style_transfer/fast_neural_style) model in ONNX Zoo, along with pre and post processors.

#### Language Models with Generative Interfaces

In the BERT example you can see how we implemented [BERT](https://github.com/onnx/models/tree/main/validated/text/machine_comprehension/bert-squad) in Mendix, complete with pre/post processors and tokenizers.

### Training your own model

In the [notebooks](notebooks) folder you can find examples on how to create models for later usage. For the other models, as specified on each demo app, you may need to refer to the [ONNX Model Zoo](https://github.com/onnx/models)

## Where to go next

Sky is the limit! Once you master how to use MLKit, you may want to create your own models (Refer to the [Training your own model](#training-your-own-model) section for examples), go to the [ONNX Model Zoo](https://github.com/onnx/models) or take a look at [Hugging Face](https://huggingface.co/) and how they, like us, use [ONNX](https://huggingface.co/docs/transformers/serialization) for fast deploying your models into production.

## Known Issues

If you are having errors launching the app. Update MendixSSO and CommunityCommons packages from marketplace
